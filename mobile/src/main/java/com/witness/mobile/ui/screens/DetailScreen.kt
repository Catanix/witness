package com.witness.mobile.ui.screens

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witness.core.data.Recording
import com.witness.core.data.Status
import com.witness.core.theme.Catppuccin
import com.witness.core.ui.IconBack
import com.witness.core.ui.IconCheck
import com.witness.core.ui.IconCopy
import com.witness.core.ui.IconPause
import com.witness.core.ui.IconPlay
import com.witness.core.ui.IconShare
import com.witness.core.ui.IconSkipBack
import com.witness.core.ui.IconSkipForward
import com.witness.mobile.Processing
import com.witness.mobile.R
import com.witness.mobile.WitnessApp
import com.witness.mobile.share.ShareRecording
import com.witness.mobile.ui.GhostIconBtn
import com.witness.mobile.ui.MonoMeta
import com.witness.mobile.ui.SectionLabel
import com.witness.mobile.ui.StatusPill
import com.witness.mobile.ui.Waveform
import com.witness.mobile.ui.fmtClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab { SUMMARY, POINTS, TASKS, TRANSCRIPT }

@Composable
fun DetailScreen(id: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as WitnessApp
    val items by app.store.items.collectAsState()
    val rec = items.find { it.id == id }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var tab by remember { mutableStateOf(Tab.SUMMARY) }
    var shareMenu by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    var playing by remember { mutableStateOf(false) }
    var posMs by remember { mutableStateOf(0) }
    var durMs by remember { mutableStateOf(0) }
    var speed by remember { mutableStateOf(1f) }
    val player = remember(rec?.id) {
        if (rec == null) null else runCatching {
            MediaPlayer().apply {
                setDataSource(rec.path)
                setOnPreparedListener { durMs = it.duration; it.seekTo(posMs) }
                setOnCompletionListener { playing = false; posMs = 0 }
                prepareAsync()
            }
        }.getOrNull()
    }
    DisposableEffect(rec?.id) { onDispose { runCatching { player?.release() } } }
    LaunchedEffect(playing) {
        while (playing) {
            posMs = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
            delay(200)
        }
    }
    fun seek(ms: Int) {
        val clamped = ms.coerceIn(0, if (durMs > 0) durMs else Int.MAX_VALUE)
        posMs = clamped
        runCatching { player?.seekTo(clamped) }
    }
    fun togglePlay() {
        val p = player ?: return
        if (playing) { runCatching { p.pause() }; playing = false }
        else { runCatching { p.playbackParams = PlaybackParams().setSpeed(speed) }; runCatching { p.start() }; playing = true }
    }

    if (rec == null) { onBack(); return }
    val dateStr = remember(rec.createdAt) { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(rec.createdAt)) }

    Box(Modifier.fillMaxSize().background(Catppuccin.Base).statusBarsPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                GhostIconBtn(onClick = onBack) { IconBack(Catppuccin.Text, Modifier.size(20.dp)) }
                Spacer(Modifier.weight(1f))
                if (rec.status == Status.DONE) GhostIconBtn(onClick = { shareMenu = true }) { IconShare(Catppuccin.Blue, Modifier.size(18.dp)) }
            }
            Text(rec.title.ifBlank { stringResource(R.string.untitled) }, color = Catppuccin.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dateStr, style = MonoMeta)
                Spacer(Modifier.width(10.dp))
                Text(fmtClock(rec.durationSec), style = MonoMeta)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(when (rec.source) { com.witness.core.data.Source.WATCH -> R.string.source_watch; com.witness.core.data.Source.PHONE -> R.string.source_phone; com.witness.core.data.Source.IMPORT -> R.string.source_import }), color = Catppuccin.Overlay0, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                StatusPill(rec.status, stringResource(when (rec.status) { Status.DONE -> R.string.status_done; Status.RECORDED -> R.string.status_recorded; Status.TRANSCRIBING -> R.string.status_transcribing; Status.FAILED -> R.string.status_failed }))
            }
            Spacer(Modifier.height(16.dp))

            // плеер
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Catppuccin.Mantle)
                .border(1.dp, Catppuccin.Surface0, RoundedCornerShape(20.dp)).padding(16.dp)) {
                val total = if (durMs > 0) durMs else rec.durationSec * 1000
                Waveform(
                    progress = if (total > 0) posMs.toFloat() / total else 0f,
                    onScrub = { f -> seek((f * total).toInt()) },
                    markers = if (total > 0) rec.markers.map { it * 1000f / total } else emptyList(),
                    onMarkerTap = { i -> seek(rec.markers[i] * 1000) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row { Text(fmtClock(posMs / 1000), style = MonoMeta); Spacer(Modifier.weight(1f)); Text(fmtClock(total / 1000), style = MonoMeta) }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth()) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        GhostIconBtn(onClick = { seek(posMs - 15000) }) { IconSkipBack(Catppuccin.Subtext0, Modifier.size(20.dp)) }
                        Spacer(Modifier.width(18.dp))
                        Box(Modifier.size(52.dp).clip(CircleShape).background(Catppuccin.Mauve).clickable { togglePlay() }, contentAlignment = Alignment.Center) {
                            if (playing) IconPause(Catppuccin.Crust, Modifier.size(22.dp)) else IconPlay(Catppuccin.Crust, Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(18.dp))
                        GhostIconBtn(onClick = { seek(posMs + 15000) }) { IconSkipForward(Catppuccin.Subtext0, Modifier.size(20.dp)) }
                    }
                    Box(Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(10.dp)).background(Catppuccin.Surface0)
                        .clickable { speed = when (speed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }; runCatching { player?.playbackParams = PlaybackParams().setSpeed(speed) } }
                        .padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text("${speed}x", color = Catppuccin.Subtext1, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            when (rec.status) {
                Status.TRANSCRIBING, Status.RECORDED -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Catppuccin.Mauve, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.detail_processing), color = Catppuccin.Yellow, fontSize = 14.sp)
                }
                Status.FAILED -> Column {
                    Text(rec.error, color = Catppuccin.Red, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Catppuccin.Surface0).clickable { scope.launch { Processing.start(context, rec) } }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(stringResource(R.string.detail_retry), color = Catppuccin.Text, fontSize = 14.sp)
                    }
                }
                Status.DONE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TabChip(stringResource(R.string.tab_summary), tab == Tab.SUMMARY) { tab = Tab.SUMMARY }
                        TabChip(stringResource(R.string.tab_points), tab == Tab.POINTS) { tab = Tab.POINTS }
                        TabChip(stringResource(R.string.tab_tasks), tab == Tab.TASKS) { tab = Tab.TASKS }
                        TabChip(stringResource(R.string.tab_transcript), tab == Tab.TRANSCRIPT) { tab = Tab.TRANSCRIPT }
                    }
                    Spacer(Modifier.height(14.dp))
                    when (tab) {
                        Tab.SUMMARY -> Column {
                            Text(rec.summary.ifBlank { stringResource(R.string.detail_no_summary) }, color = Catppuccin.Subtext1, fontSize = 15.sp, lineHeight = 23.sp)
                            if (rec.points.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp)); SectionLabel(stringResource(R.string.tab_points))
                                rec.points.forEach { p -> PointRow(p, 0) {} }
                            }
                        }
                        Tab.POINTS -> Column {
                            if (rec.points.isEmpty()) Text(stringResource(R.string.detail_no_points), color = Catppuccin.Overlay0, fontSize = 14.sp)
                            rec.points.forEach { p -> PointRow(p, 0) {} }
                        }
                        Tab.TASKS -> Column {
                            if (rec.tasks.isEmpty()) Text(stringResource(R.string.detail_no_tasks), color = Catppuccin.Overlay0, fontSize = 14.sp)
                            rec.tasks.forEachIndexed { i, t ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Catppuccin.Mantle)
                                    .clickable { scope.launch { app.store.update(rec.id) { r -> r.copy(tasks = r.tasks.mapIndexed { j, x -> if (j == i) x.copy(done = !x.done) else x }) } } }
                                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                                        .background(if (t.done) Catppuccin.Green else Catppuccin.Surface0)
                                        .border(1.dp, if (t.done) Catppuccin.Green else Catppuccin.Surface2, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                        if (t.done) IconCheck(Catppuccin.Crust, Modifier.size(13.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(t.text, color = if (t.done) Catppuccin.Overlay0 else Catppuccin.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        Tab.TRANSCRIPT -> Column {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                SectionLabel(stringResource(R.string.tab_transcript))
                                Spacer(Modifier.weight(1f))
                                GhostIconBtn(onClick = {
                                    clipboard.setText(AnnotatedString(rec.transcript)); copied = true
                                    scope.launch { delay(1500); copied = false }
                                }) { if (copied) IconCheck(Catppuccin.Green, Modifier.size(16.dp)) else IconCopy(Catppuccin.Subtext0, Modifier.size(16.dp)) }
                            }
                            if (rec.segments.isEmpty()) Text(stringResource(R.string.detail_no_transcript), color = Catppuccin.Overlay0, fontSize = 14.sp)
                            rec.segments.forEach { seg ->
                                Row(Modifier.fillMaxWidth().clickable { seek(seg.startMs.toInt()) }.padding(vertical = 7.dp)) {
                                    Text(fmtClock((seg.startMs / 1000).toInt()), style = MonoMeta, modifier = Modifier.width(52.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(seg.text, color = Catppuccin.Subtext1, fontSize = 14.sp, lineHeight = 21.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (shareMenu) {
            Box(Modifier.fillMaxSize().background(Catppuccin.Crust.copy(alpha = 0.5f)).clickable { shareMenu = false })
            Column(Modifier.align(Alignment.BottomEnd).padding(20.dp).navigationBarsPadding()
                .clip(RoundedCornerShape(16.dp)).background(Catppuccin.Surface0)
                .border(1.dp, Catppuccin.Surface2, RoundedCornerShape(16.dp)).padding(6.dp).width(240.dp)) {
                ShareItem(stringResource(R.string.detail_share_brief)) { shareMenu = false; ShareRecording.send(context, rec, ShareRecording.Mode.BRIEF) }
                ShareItem(stringResource(R.string.detail_share_full)) { shareMenu = false; ShareRecording.send(context, rec, ShareRecording.Mode.FULL) }
                ShareItem(stringResource(R.string.detail_share_md)) { shareMenu = false; ShareRecording.send(context, rec, ShareRecording.Mode.MD) }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, active: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (active) Catppuccin.Mauve else Catppuccin.Overlay0, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        Box(Modifier.height(2.dp).width(28.dp).clip(RoundedCornerShape(1.dp)).background(if (active) Catppuccin.Mauve else Catppuccin.Surface0))
    }
}

@Composable
private fun PointRow(text: String, timeSec: Int, onSeek: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(Catppuccin.Mauve))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Catppuccin.Subtext1, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
        if (timeSec > 0) { Spacer(Modifier.width(8.dp)); Text(fmtClock(timeSec), style = MonoMeta, modifier = Modifier.clickable(onClick = onSeek)) }
    }
}

@Composable
private fun ShareItem(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconShare(Catppuccin.Blue, Modifier.size(18.dp)); Spacer(Modifier.width(12.dp)); Text(label, color = Catppuccin.Text, fontSize = 15.sp)
    }
}
