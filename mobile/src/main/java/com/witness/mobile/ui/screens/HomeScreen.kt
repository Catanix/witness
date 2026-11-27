package com.witness.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.witness.core.data.Recording
import com.witness.core.data.Source
import com.witness.core.data.Status
import com.witness.core.theme.Catppuccin
import com.witness.core.ui.IconGear
import com.witness.core.ui.IconImport
import com.witness.core.ui.IconMic
import com.witness.core.ui.IconPlus
import com.witness.core.ui.IconSearch
import com.witness.core.ui.IconShare
import com.witness.core.ui.IconTrash
import com.witness.core.ui.LogoMark
import com.witness.mobile.Processing
import com.witness.mobile.R
import com.witness.mobile.WitnessApp
import com.witness.mobile.share.ShareRecording
import com.witness.mobile.ui.FilterChip
import com.witness.mobile.ui.GhostIconBtn
import com.witness.mobile.ui.MonoMeta
import com.witness.mobile.ui.SourceIcon
import com.witness.mobile.ui.StatusPill
import com.witness.mobile.ui.fmtClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class Group { TODAY, YESTERDAY, WEEK, EARLIER }

private fun groupOf(ts: Long): Group {
    val now = Calendar.getInstance()
    val day = Calendar.getInstance().apply { timeInMillis = ts }
    val startToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val diffDays = ((startToday.timeInMillis - day.timeInMillis) / 86_400_000L).toInt()
    return when {
        day.get(Calendar.YEAR) == now.get(Calendar.YEAR) && day.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> Group.TODAY
        diffDays == 0 -> Group.YESTERDAY
        diffDays in 1..6 -> Group.WEEK
        else -> Group.EARLIER
    }
}

@Composable
fun HomeScreen(onRecord: () -> Unit, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as WitnessApp
    val items by app.store.items.collectAsState()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<Source?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var cardMenu by remember { mutableStateOf<Recording?>(null) }
    var renameTarget by remember { mutableStateOf<Recording?>(null) }
    var renameText by remember { mutableStateOf("") }
    var shareMode by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(setOf<String>()) }
    var pendingDelete by remember { mutableStateOf<Recording?>(null) }

    // отложенное удаление с возможностью Undo
    LaunchedEffect(pendingDelete) {
        val target = pendingDelete ?: return@LaunchedEffect
        delay(4000)
        if (pendingDelete?.id == target.id) {
            app.store.delete(target.id)
            hidden = hidden - target.id
            pendingDelete = null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val rec = withContext(Dispatchers.IO) {
                try {
                    var name = "audio"
                    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && c.moveToFirst()) name = c.getString(idx)
                    }
                    val ext = name.substringAfterLast('.', "wav").lowercase()
                    val dir = File(context.filesDir, "audio").apply { mkdirs() }
                    val id = "imp_${System.currentTimeMillis()}"
                    val out = File(dir, "$id.$ext")
                    context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
                        ?: return@withContext null
                    val dur = if (ext == "wav") ((out.length() - 44) / 32000).toInt().coerceAtLeast(0) else 0
                    Recording(id, out.absolutePath, System.currentTimeMillis(), dur, Source.IMPORT)
                } catch (_: Exception) { null }
            }
            if (rec != null) Processing.start(context, rec)
        }
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) onRecord() }

    val visible = items.filter { it.id !in hidden }
        .filter { filter == null || it.source == filter }
        .filter { q -> query.isBlank() || q.title.contains(query, true) || q.summary.contains(query, true) || q.transcript.contains(query, true) }
    val grouped = visible.groupBy { groupOf(it.createdAt) }

    Box(Modifier.fillMaxSize().background(Catppuccin.Base).statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LogoMark(Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("WITNESS", color = Catppuccin.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                GhostIconBtn(onClick = onSettings) { IconGear(Catppuccin.Subtext0, Modifier.size(19.dp)) }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(14.dp))
                    .background(Catppuccin.Mantle).border(1.dp, Catppuccin.Surface0, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconSearch(Catppuccin.Overlay0, Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.search_hint), color = Catppuccin.Overlay0, fontSize = 14.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Catppuccin.Text, fontSize = 14.sp),
                        cursorBrush = SolidColor(Catppuccin.Mauve),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(stringResource(R.string.filter_all), filter == null, onClick = { filter = null })
                FilterChip(stringResource(R.string.filter_watch), filter == Source.WATCH, onClick = { filter = Source.WATCH })
                FilterChip(stringResource(R.string.filter_phone), filter == Source.PHONE, onClick = { filter = Source.PHONE })
            }
            Spacer(Modifier.height(8.dp))

            if (visible.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(if (items.isEmpty()) R.string.home_empty_title else R.string.no_results), color = Catppuccin.Overlay1, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.home_empty_sub), color = Catppuccin.Overlay0, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (g in listOf(Group.TODAY, Group.YESTERDAY, Group.WEEK, Group.EARLIER)) {
                        val list = grouped[g] ?: continue
                        item(key = "h_$g") {
                            Text(
                                stringResource(when (g) { Group.TODAY -> R.string.group_today; Group.YESTERDAY -> R.string.group_yesterday; Group.WEEK -> R.string.group_week; Group.EARLIER -> R.string.group_earlier }),
                                color = Catppuccin.Overlay0, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }
                        items(list, key = { it.id }) { rec ->
                            Card(rec, onClick = { onOpen(rec.id) }, onLongClick = { shareMode = false; cardMenu = rec })
                        }
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp).navigationBarsPadding()) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Catppuccin.Mauve)
                .clickable { menuOpen = true }.semantics { contentDescription = "add" }, contentAlignment = Alignment.Center) {
                IconPlus(Catppuccin.Crust, Modifier.size(24.dp))
            }
            if (menuOpen) {
                Column(Modifier.align(Alignment.TopEnd).offset(y = (-12).dp)
                    .clip(RoundedCornerShape(16.dp)).background(Catppuccin.Surface0)
                    .border(1.dp, Catppuccin.Surface2, RoundedCornerShape(16.dp)).padding(6.dp).width(220.dp)) {
                    MenuItem(stringResource(R.string.menu_record), { IconMic(Catppuccin.Mauve, Modifier.size(20.dp)) }) {
                        menuOpen = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) onRecord()
                        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    MenuItem(stringResource(R.string.menu_import), { IconImport(Catppuccin.Blue, Modifier.size(20.dp)) }) {
                        menuOpen = false; importLauncher.launch(arrayOf("audio/*"))
                    }
                }
            }
        }

        val target = cardMenu
        if (target != null) {
            Box(Modifier.fillMaxSize().background(Catppuccin.Crust.copy(alpha = 0.5f)).clickable { cardMenu = null })
            Column(Modifier.align(Alignment.BottomEnd).padding(20.dp).navigationBarsPadding()
                .clip(RoundedCornerShape(16.dp)).background(Catppuccin.Surface0)
                .border(1.dp, Catppuccin.Surface2, RoundedCornerShape(16.dp)).padding(6.dp).width(240.dp)) {
                if (!shareMode) {
                    if (target.status == Status.DONE) {
                        MenuItem(stringResource(R.string.card_share), { IconShare(Catppuccin.Blue, Modifier.size(20.dp)) }) { shareMode = true }
                    }
                    MenuItem(stringResource(R.string.card_rename), { IconPencil(Catppuccin.Yellow, Modifier.size(20.dp)) }) {
                        renameText = target.title
                        renameTarget = target
                        cardMenu = null
                    }
                    MenuItem(stringResource(R.string.card_delete), { IconTrash(Catppuccin.Red, Modifier.size(20.dp)) }) {
                        cardMenu = null
                        hidden = hidden + target.id
                        pendingDelete = target
                    }
                } else {
                    MenuItem(stringResource(R.string.detail_share_brief), { IconShare(Catppuccin.Blue, Modifier.size(20.dp)) }) { cardMenu = null; ShareRecording.send(context, target, ShareRecording.Mode.BRIEF) }
                    MenuItem(stringResource(R.string.detail_share_full), { IconShare(Catppuccin.Blue, Modifier.size(20.dp)) }) { cardMenu = null; ShareRecording.send(context, target, ShareRecording.Mode.FULL) }
                    MenuItem(stringResource(R.string.detail_share_md), { IconShare(Catppuccin.Blue, Modifier.size(20.dp)) }) { cardMenu = null; ShareRecording.send(context, target, ShareRecording.Mode.MD) }
                }
            }
        }

        // шит переименования (в стиле стоп-шита)
        val rt = renameTarget
        if (rt != null) {
            Box(Modifier.fillMaxSize().background(Catppuccin.Crust.copy(alpha = 0.5f)).clickable { renameTarget = null })
            Column(
                Modifier.align(Alignment.BottomCenter).padding(10.dp).navigationBarsPadding().imePadding().fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)).background(Catppuccin.Mantle)
                    .border(1.dp, Catppuccin.Surface0, RoundedCornerShape(24.dp)).padding(8.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp, bottom = 10.dp)
                    .size(width = 38.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Catppuccin.Surface2))
                Text(
                    stringResource(R.string.card_rename).uppercase(),
                    color = Catppuccin.Overlay1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 8.dp),
                )
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).clip(RoundedCornerShape(12.dp))
                        .background(Catppuccin.Base).border(1.dp, Catppuccin.Surface1, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = renameText, onValueChange = { renameText = it }, singleLine = true,
                        textStyle = TextStyle(color = Catppuccin.Text, fontSize = 14.sp),
                        cursorBrush = SolidColor(Catppuccin.Mauve),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "rename_field" },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Catppuccin.Surface0)
                            .border(1.dp, Catppuccin.Surface2, RoundedCornerShape(12.dp))
                            .clickable { renameTarget = null }.padding(vertical = 13.dp)
                            .semantics { contentDescription = "rename_cancel" },
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.recording_cancel), color = Catppuccin.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Catppuccin.Mauve)
                            .clickable {
                                val newTitle = renameText.trim()
                                scope.launch { app.store.update(rt.id) { it.copy(title = newTitle) } }
                                renameTarget = null
                            }.padding(vertical = 13.dp)
                            .semantics { contentDescription = "rename_save" },
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.settings_save), color = Catppuccin.Crust, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        if (pendingDelete != null) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp).navigationBarsPadding()
                    .clip(RoundedCornerShape(14.dp)).background(Catppuccin.Surface0)
                    .border(1.dp, Catppuccin.Surface2, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.deleted), color = Catppuccin.Text, fontSize = 14.sp)
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.undo), color = Catppuccin.Mauve, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        pendingDelete?.let { hidden = hidden - it.id }
                        pendingDelete = null
                    })
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        icon(); Spacer(Modifier.width(12.dp)); Text(label, color = Catppuccin.Text, fontSize = 15.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Card(rec: Recording, onClick: () -> Unit, onLongClick: () -> Unit) {
    val time = remember(rec.createdAt) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(rec.createdAt)) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Catppuccin.Mantle)
            .border(1.dp, Catppuccin.Surface0, RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Catppuccin.Surface0), contentAlignment = Alignment.Center) {
                SourceIcon(rec.source, Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(rec.title.ifBlank { stringResource(R.string.untitled) }, color = Catppuccin.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (rec.summary.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(rec.summary, color = Catppuccin.Overlay1, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(10.dp))
            StatusPill(rec.status, stringResource(when (rec.status) { Status.DONE -> R.string.status_done; Status.RECORDED -> R.string.status_recorded; Status.TRANSCRIBING -> R.string.status_transcribing; Status.FAILED -> R.string.status_failed }))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time, style = MonoMeta)
            Spacer(Modifier.width(10.dp))
            Text(fmtClock(rec.durationSec), style = MonoMeta)
            Spacer(Modifier.weight(1f))
            Text(stringResource(when (rec.source) { Source.WATCH -> R.string.source_watch; Source.PHONE -> R.string.source_phone; Source.IMPORT -> R.string.source_import }), color = Catppuccin.Overlay0, fontSize = 11.sp)
        }
    }
}

/** Карандаш для пункта «Переименовать» (локальная иконка, общий Icons.kt не трогаем). */
@Composable
private fun IconPencil(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        val p = Path().apply {
            moveTo(s * 0.25f, s * 0.75f)
            lineTo(s * 0.70f, s * 0.30f)
            lineTo(s * 0.80f, s * 0.40f)
            lineTo(s * 0.35f, s * 0.85f)
            close()
        }
        drawPath(p, color, style = Stroke(width = s * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(s * 0.55f, s * 0.88f), Offset(s * 0.88f, s * 0.88f), s * 0.08f, StrokeCap.Round)
    }
}
