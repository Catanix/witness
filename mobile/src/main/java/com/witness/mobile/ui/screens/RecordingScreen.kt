package com.witness.mobile.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.witness.core.theme.Catppuccin
import com.witness.core.ui.IconCheck
import com.witness.core.ui.IconFlag
import com.witness.core.ui.IconPause
import com.witness.core.ui.IconPlay
import com.witness.core.ui.IconPlus
import com.witness.core.ui.IconStop
import com.witness.core.ui.IconTrash
import com.witness.core.ui.IconX
import com.witness.mobile.R
import com.witness.mobile.record.RecordingService
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun RecordingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val rec by RecordingService.recorder.collectAsState()
    val amplitude = rec?.amplitude?.collectAsState()?.value ?: 0f
    val elapsed = rec?.elapsedSec?.collectAsState()?.value ?: 0L
    val paused = rec?.paused?.collectAsState()?.value ?: false
    val saving by RecordingService.saving.collectAsState()
    var denied by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var toastTick by remember { mutableStateOf(0) }
    var toastVisible by remember { mutableStateOf(false) }
    var hadRec by remember { mutableStateOf(false) }

    val started = rec != null

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) RecordingService.send(context, RecordingService.ACTION_START) else denied = true
    }
    LaunchedEffect(Unit) {
        if (RecordingService.recorder.value == null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                RecordingService.send(context, RecordingService.ACTION_START)
            } else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    // сервис завершился (сохранили/удалили) -> назад на главную
    LaunchedEffect(rec) {
        if (rec != null) hadRec = true else if (hadRec) onDone()
    }
    // назад = свернуть приложение, запись продолжается в шторке
    BackHandler(enabled = started && !saving) { (context as? Activity)?.moveTaskToBack(true) }

    LaunchedEffect(toastTick) {
        if (toastTick > 0) {
            toastVisible = true
            delay(2600)
            toastVisible = false
        }
    }

    val phase by rememberInfiniteTransition(label = "eq").animateFloat(0f, 6.283f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "ph")
    val dotAlpha by rememberInfiniteTransition(label = "live").animateFloat(1f, 0.4f,
        infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse), label = "dot")

    Box(Modifier.fillMaxSize().background(Catppuccin.Base).statusBarsPadding(), contentAlignment = Alignment.Center) {
        if (denied) {
            Text(stringResource(R.string.recording_mic_denied), color = Catppuccin.Red, fontSize = 15.sp)
        } else {
            Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Catppuccin.Red.copy(alpha = dotAlpha)))
                    Text(stringResource(R.string.rec_live), color = Catppuccin.Red, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "%02d:%02d".format(elapsed / 60, elapsed % 60),
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
                    fontSize = 56.sp, letterSpacing = (-1).sp, color = Catppuccin.Text,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (paused) stringResource(R.string.recording_paused) else stringResource(R.string.rec_state),
                    color = if (paused) Catppuccin.Yellow else Catppuccin.Mauve,
                    fontSize = 13.5.sp, fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(38.dp))
                Row(Modifier.height(96.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (i in 0 until 28) {
                        val target = if (paused) 0.06f else (amplitude * (0.35f + 0.65f * abs(sin(phase + i * 0.7f)))).coerceIn(0.06f, 1f)
                        val h by animateFloatAsState(target, tween(120), label = "b$i")
                        Box(Modifier.width(5.dp).fillMaxHeight(h).clip(RoundedCornerShape(3.dp))
                            .background(if (paused) Catppuccin.Overlay0 else Catppuccin.Mauve))
                    }
                }
                Spacer(Modifier.height(38.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(Catppuccin.Surface0)
                        .clickable(enabled = started && !saving) { RecordingService.send(context, RecordingService.ACTION_MARK); toastTick++ }
                        .semantics { contentDescription = "marker" }, contentAlignment = Alignment.Center) {
                        IconFlag(Catppuccin.Subtext0, Modifier.size(22.dp))
                    }
                    Box(Modifier.size(78.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(Catppuccin.Red)
                        .clickable(enabled = started && !saving) { confirm = true }
                        .semantics { contentDescription = "stop" }, contentAlignment = Alignment.Center) {
                        IconStop(Catppuccin.Crust, Modifier.size(28.dp))
                    }
                    Box(Modifier.size(54.dp).clip(CircleShape).background(Catppuccin.Surface0)
                        .clickable(enabled = started && !saving) {
                            RecordingService.send(context, if (paused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE)
                        }
                        .semantics { contentDescription = if (paused) "resume" else "pause" }, contentAlignment = Alignment.Center) {
                        if (paused) IconPlay(Catppuccin.Subtext0, Modifier.size(22.dp)) else IconPause(Catppuccin.Subtext0, Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(30.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Catppuccin.Mantle)
                        .border(1.dp, Catppuccin.Surface0, RoundedCornerShape(50))
                        .clickable(enabled = started && !saving) { RecordingService.send(context, RecordingService.ACTION_MARK); toastTick++ }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                        .semantics { contentDescription = "mark_moment" },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    IconPlus(Catppuccin.Yellow, Modifier.size(15.dp))
                    Text(stringResource(R.string.rec_mark), color = Catppuccin.Subtext0, fontSize = 12.5.sp)
                }
            }
        }

        if (toastVisible) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).navigationBarsPadding()
                    .clip(RoundedCornerShape(14.dp)).background(Catppuccin.Surface0)
                    .border(1.dp, Catppuccin.Surface2, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.toast_marker), color = Catppuccin.Text, fontSize = 14.sp)
            }
        }

        if (confirm) {
            Box(Modifier.fillMaxSize().background(Catppuccin.Crust.copy(alpha = 0.5f)).clickable { confirm = false })
            Column(
                Modifier.align(Alignment.BottomCenter).padding(10.dp).navigationBarsPadding().fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)).background(Catppuccin.Mantle)
                    .border(1.dp, Catppuccin.Surface0, RoundedCornerShape(24.dp)).padding(8.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp, bottom = 10.dp)
                    .size(width = 38.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Catppuccin.Surface2))
                Text(
                    stringResource(R.string.recording_confirm_stop).uppercase(),
                    color = Catppuccin.Overlay1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 8.dp),
                )
                SheetRow(desc = "save", onClick = {
                    if (!saving) RecordingService.send(context, RecordingService.ACTION_STOP_SAVE)
                }) {
                    IconTile(Color(0x2EA6E3A1)) { IconCheck(Catppuccin.Green, Modifier.size(20.dp)) }
                    Column {
                        Text(stringResource(R.string.stop_save), color = Catppuccin.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(1.dp))
                        Text(stringResource(R.string.stop_save_sub), color = Catppuccin.Overlay0, fontSize = 11.5.sp)
                    }
                }
                SheetRow(desc = "continue_rec", onClick = { confirm = false }) {
                    IconTile(Catppuccin.Surface0) { IconX(Catppuccin.Subtext0, Modifier.size(20.dp)) }
                    Text(stringResource(R.string.stop_continue), color = Catppuccin.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                SheetRow(desc = "discard", onClick = {
                    if (!saving) RecordingService.send(context, RecordingService.ACTION_DISCARD)
                }) {
                    IconTile(Color(0x26F38BA8)) { IconTrash(Catppuccin.Red, Modifier.size(18.dp)) }
                    Text(stringResource(R.string.stop_discard), color = Catppuccin.Red, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SheetRow(desc: String, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .semantics { contentDescription = desc },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
        content = content,
    )
}

@Composable
private fun IconTile(bg: Color, content: @Composable () -> Unit) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) { content() }
}
