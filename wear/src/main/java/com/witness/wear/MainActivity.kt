package com.witness.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.witness.core.theme.Catppuccin
import com.witness.core.ui.IconCheck
import com.witness.core.ui.IconPause
import com.witness.core.ui.IconPlay
import com.witness.core.ui.IconStop
import com.witness.core.ui.IconX
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WearRoot() } }
    }
}

private enum class WState { IDLE, REC, SAVED }

@Composable
fun WearRoot() {
    val context = LocalContext.current
    val rec by WearRecordingService.recorder.collectAsState()
    val saving by WearRecordingService.saving.collectAsState()
    val lastSaved by WearRecordingService.lastSaved.collectAsState()
    val pending by SyncQueue.pendingCount.collectAsState()
    val amplitude = rec?.amplitude?.collectAsState()?.value ?: 0f
    val elapsed = rec?.elapsedSec?.collectAsState()?.value ?: 0L
    val paused = rec?.paused?.collectAsState()?.value ?: false

    var state by remember { mutableStateOf(WState.IDLE) }
    var overlay by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var hadRec by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) WearRecordingService.send(context, WearRecordingService.ACTION_START) else denied = true
    }
    fun startRec() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            WearRecordingService.send(context, WearRecordingService.ACTION_START)
        } else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(rec) {
        if (rec != null) { hadRec = true; state = WState.REC }
        else if (hadRec) {
            hadRec = false
            overlay = false
            state = if (lastSaved) WState.SAVED else WState.IDLE
        }
    }
    LaunchedEffect(state) {
        if (state == WState.SAVED) { delay(1100); state = WState.IDLE }
    }
    BackHandler(enabled = state == WState.REC) { overlay = !overlay }

    val phase by rememberInfiniteTransition(label = "eq").animateFloat(0f, 6.283f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "ph")
    val dotAlpha by rememberInfiniteTransition(label = "live").animateFloat(1f, 0.35f,
        infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse), label = "dot")
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(1f, 1.05f,
        infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse), label = "br")

    Box(Modifier.fillMaxSize().background(Catppuccin.Base), contentAlignment = Alignment.Center) {
        when (state) {
            WState.IDLE -> {
                // весь экран — зона нажатия
                Column(
                    Modifier.fillMaxSize().clickable(enabled = !saving) { startRec() }.semantics { contentDescription = "rec" },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(126.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size((124 * breath).dp).clip(CircleShape).border(1.dp, Catppuccin.Lavender.copy(alpha = 0.10f), CircleShape))
                        Box(Modifier.size((104 * breath).dp).clip(CircleShape).border(1.dp, Catppuccin.Lavender.copy(alpha = 0.20f), CircleShape))
                        Box(
                            Modifier.size(80.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Catppuccin.Mauve, Catppuccin.Lavender))),
                            contentAlignment = Alignment.Center,
                        ) { Box(Modifier.size(30.dp).clip(CircleShape).background(Catppuccin.Crust)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.w_idle_hint), color = Catppuccin.Subtext0, fontSize = 12.5.sp)
                }
                if (pending > 0) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(50)).background(Catppuccin.Mantle)
                            .border(1.dp, Catppuccin.Surface0, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(stringResource(R.string.w_queue, pending), color = Catppuccin.Overlay1, fontSize = 9.sp)
                    }
                }
                if (denied) {
                    Text(stringResource(R.string.mic_denied), color = Catppuccin.Red, fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp))
                }
            }
            WState.REC -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape)
                            .background(if (paused) Catppuccin.Yellow else Catppuccin.Red.copy(alpha = dotAlpha)))
                        Text(
                            if (paused) stringResource(R.string.pause) else stringResource(R.string.w_live),
                            color = if (paused) Catppuccin.Yellow else Catppuccin.Red,
                            fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "%02d:%02d".format(elapsed / 60, elapsed % 60),
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
                        fontSize = 32.sp, letterSpacing = (-0.5).sp,
                        color = if (paused) Catppuccin.Yellow else Catppuccin.Text,
                    )
                    Spacer(Modifier.height(8.dp))
                    // эквалайзер во всю ширь
                    Row(Modifier.height(46.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 0 until 18) {
                            val target = if (paused) 0.10f else (amplitude * (0.35f + 0.65f * abs(sin(phase + i * 0.7f)))).coerceIn(0.10f, 1f)
                            val h by animateFloatAsState(target, tween(120), label = "b$i")
                            Box(Modifier.width(5.dp).fillMaxHeight(h).clip(RoundedCornerShape(2.5.dp))
                                .background(if (paused) Catppuccin.Overlay0 else Catppuccin.Mauve))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (paused) {
                        BigPill("resume", Catppuccin.Green, stringResource(R.string.resume), { IconPlay(Catppuccin.Crust, Modifier.size(20.dp)) }) {
                            WearRecordingService.send(context, WearRecordingService.ACTION_RESUME)
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(Catppuccin.Surface0)
                                .clickable(enabled = !saving) { overlay = true }
                                .padding(horizontal = 18.dp, vertical = 7.dp)
                                .semantics { contentDescription = "stop" },
                        ) { Text(stringResource(R.string.stop), color = Catppuccin.Subtext0, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) }
                    } else {
                        BigPill("stop", Catppuccin.Red, stringResource(R.string.stop), { IconStop(Catppuccin.Crust, Modifier.size(20.dp)) }) {
                            overlay = true
                        }
                    }
                }
                // полноэкранный оверлей стопа
                if (overlay) {
                    Box(Modifier.fillMaxSize().background(Catppuccin.Crust.copy(alpha = 0.96f)).clickable { overlay = false })
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        OverlayBtn("ov_pause", Catppuccin.Surface0, Catppuccin.Yellow, stringResource(R.string.pause),
                            { IconPause(Catppuccin.Yellow, Modifier.size(18.dp)) }, border = Catppuccin.Surface2) {
                            WearRecordingService.send(context, WearRecordingService.ACTION_PAUSE)
                            overlay = false
                        }
                        Spacer(Modifier.height(8.dp))
                        OverlayBtn("ov_save", Catppuccin.Green, Catppuccin.Crust, stringResource(R.string.save),
                            { IconStop(Catppuccin.Crust, Modifier.size(18.dp)) }) {
                            if (!saving) WearRecordingService.send(context, WearRecordingService.ACTION_STOP_SAVE)
                        }
                        Spacer(Modifier.height(8.dp))
                        OverlayBtn("ov_cancel", Color.Transparent, Catppuccin.Red, stringResource(R.string.cancel),
                            { IconX(Catppuccin.Red, Modifier.size(18.dp)) }, border = Catppuccin.Red.copy(alpha = 0.45f)) {
                            if (!saving) WearRecordingService.send(context, WearRecordingService.ACTION_DISCARD)
                        }
                    }
                }
            }
            WState.SAVED -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Catppuccin.Green.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        IconCheck(Catppuccin.Green, Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.w_saved), color = Catppuccin.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text(stringResource(R.string.w_autosend), color = Catppuccin.Subtext0, fontSize = 10.5.sp)
                }
            }
        }
    }
}

@Composable
private fun BigPill(desc: String, bg: Color, label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.width(136.dp).height(46.dp).clip(RoundedCornerShape(23.dp)).background(bg)
            .clickable(onClick = onClick).semantics { contentDescription = desc },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(label, color = Catppuccin.Crust, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OverlayBtn(desc: String, bg: Color, fg: Color, label: String, icon: @Composable () -> Unit, border: Color? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(15.dp)).background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(15.dp)) else Modifier)
            .clickable(onClick = onClick).semantics { contentDescription = desc },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.width(9.dp))
        Text(label, color = fg, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
