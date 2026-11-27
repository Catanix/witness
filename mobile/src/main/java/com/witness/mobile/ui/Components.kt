package com.witness.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witness.core.data.Status
import com.witness.core.theme.Catppuccin
import com.witness.core.ui.IconClock
import com.witness.core.ui.IconPhone
import com.witness.core.ui.IconWatch
import kotlin.math.abs
import kotlin.math.sin

fun fmtClock(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
fun fmtTimer(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)

/** Пилюля статуса: цветная точка + текст, тонированный фон (как .pill в макете). */
@Composable
fun StatusPill(status: Status, label: String, modifier: Modifier = Modifier) {
    val (fg, bg) = when (status) {
        Status.DONE -> Catppuccin.Green to Color(0x26A6E3A1)
        Status.RECORDED -> Catppuccin.Blue to Color(0x2689B4FA)
        Status.TRANSCRIBING -> Catppuccin.Yellow to Color(0x26F9E2AF)
        Status.FAILED -> Catppuccin.Red to Color(0x26F38BA8)
    }
    Row(
        modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
        Text(label, color = fg, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Иконка источника записи (часы / телефон / импорт). */
@Composable
fun SourceIcon(source: com.witness.core.data.Source, modifier: Modifier = Modifier) {
    val tint = Catppuccin.Overlay1
    when (source) {
        com.witness.core.data.Source.WATCH -> IconWatch(tint, modifier.size(18.dp))
        com.witness.core.data.Source.PHONE -> IconPhone(tint, modifier.size(18.dp))
        com.witness.core.data.Source.IMPORT -> IconClock(tint, modifier.size(18.dp))
    }
}

/** Waveform: детерминированные бары, пройденная часть lavender, остаток surface1. */
@Composable
fun Waveform(
    progress: Float,
    modifier: Modifier = Modifier,
    bars: Int = 48,
    onScrub: ((Float) -> Unit)? = null,
    markers: List<Float> = emptyList(),
    onMarkerTap: ((Int) -> Unit)? = null,
) {
    val played = Catppuccin.Lavender
    val rest = Catppuccin.Surface1
    Canvas(
        modifier
            .height(44.dp)
            .then(
                if (onScrub != null) Modifier.pointerInput(bars) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onScrub((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                } else Modifier
            )
            .then(
                if (onMarkerTap != null && markers.isNotEmpty()) Modifier.pointerInput(markers) {
                    detectTapGestures { pos ->
                        val f = pos.x / size.width
                        val idx = markers.indices.minByOrNull { abs(markers[it] - f) } ?: return@detectTapGestures
                        if (abs(markers[idx] - f) < 0.03f) onMarkerTap(idx)
                    }
                } else Modifier
            )
    ) {
        val n = bars
        val gap = 2.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val h = (0.25f + 0.75f * abs(sin(i * 1.7f) * 0.6f + sin(i * 0.53f) * 0.4f)) * size.height
            val x = i * (bw + gap)
            drawRoundRect(
                if (i <= progress * n) played else rest,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f),
            )
        }
        // метки отмеченных моментов: жёлтый тик + точка сверху, тап — перемотка
        for (f in markers) {
            val x = f.coerceIn(0f, 1f) * size.width
            drawRoundRect(Catppuccin.Yellow, topLeft = Offset(x - 1.dp.toPx(), 0f),
                size = Size(2.dp.toPx(), size.height), cornerRadius = CornerRadius(1.dp.toPx()))
            drawCircle(Catppuccin.Yellow, radius = 3.dp.toPx(), center = Offset(x, 3.dp.toPx()))
        }
    }
}

/** Фильтр-чип (pill). active = lavender-фон. */
@Composable
fun FilterChip(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Catppuccin.Lavender else Catppuccin.Surface0)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (active) Catppuccin.Crust else Catppuccin.Subtext0,
            fontSize = 12.5.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/** Заголовок секции (мелкий, разряженный, overlay). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = Catppuccin.Overlay0,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

/** Квадратная ghost-кнопка 40dp (назад / share / настройки). */
@Composable
fun GhostIconBtn(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Catppuccin.Surface0)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}