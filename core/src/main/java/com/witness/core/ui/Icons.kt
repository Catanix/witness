package com.witness.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun IconChevronLeft(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.62f, s * 0.22f), Offset(s * 0.34f, s * 0.5f), s * 0.11f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.34f, s * 0.5f), Offset(s * 0.62f, s * 0.78f), s * 0.11f, StrokeCap.Round)
    }
}

@Composable
fun IconMic(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawRoundRect(color, Offset(s * 0.36f, s * 0.10f), Size(s * 0.28f, s * 0.50f), CornerRadius(s * 0.14f))
        drawArc(color, -180f, 180f, false, Offset(s * 0.22f, s * 0.32f), Size(s * 0.56f, s * 0.44f),
            style = Stroke(width = s * 0.09f, cap = StrokeCap.Round))
        drawLine(color, Offset(s * 0.5f, s * 0.76f), Offset(s * 0.5f, s * 0.90f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.32f, s * 0.90f), Offset(s * 0.68f, s * 0.90f), s * 0.09f, StrokeCap.Round)
    }
}

@Composable
fun IconStop(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawRoundRect(color, Offset(s * 0.26f, s * 0.26f), Size(s * 0.48f, s * 0.48f), CornerRadius(s * 0.10f))
    }
}

@Composable
fun IconPause(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawRoundRect(color, Offset(s * 0.30f, s * 0.24f), Size(s * 0.13f, s * 0.52f), CornerRadius(s * 0.05f))
        drawRoundRect(color, Offset(s * 0.57f, s * 0.24f), Size(s * 0.13f, s * 0.52f), CornerRadius(s * 0.05f))
    }
}

@Composable
fun IconPlus(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.5f, s * 0.2f), Offset(s * 0.5f, s * 0.8f), s * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.8f, s * 0.5f), s * 0.10f, StrokeCap.Round)
    }
}

@Composable
fun IconImport(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.5f, s * 0.12f), Offset(s * 0.5f, s * 0.55f), s * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.32f, s * 0.38f), Offset(s * 0.5f, s * 0.56f), s * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.68f, s * 0.38f), Offset(s * 0.5f, s * 0.56f), s * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.16f, s * 0.70f), Offset(s * 0.16f, s * 0.84f), s * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.16f, s * 0.84f), Offset(s * 0.84f, s * 0.84f), s * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.84f, s * 0.84f), Offset(s * 0.84f, s * 0.70f), s * 0.10f, StrokeCap.Round)
    }
}

@Composable
fun IconShare(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.5f, s * 0.14f), Offset(s * 0.5f, s * 0.56f), s * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.32f, s * 0.30f), Offset(s * 0.5f, s * 0.13f), s * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.68f, s * 0.30f), Offset(s * 0.5f, s * 0.13f), s * 0.10f, StrokeCap.Round)
        val p = Path().apply {
            moveTo(s * 0.18f, s * 0.52f)
            lineTo(s * 0.18f, s * 0.82f)
            lineTo(s * 0.82f, s * 0.82f)
            lineTo(s * 0.82f, s * 0.52f)
        }
        drawPath(p, color, style = Stroke(width = s * 0.10f, cap = StrokeCap.Round))
    }
}

/** Логотип Witness: мини-эквалайзер в скруглённом квадрате. */
@Composable
fun LogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawRoundRect(Color(0xFFB4BEFE), cornerRadius = CornerRadius(s * 0.28f))
        val bars = listOf(0.30f, 0.52f, 0.40f, 0.60f)
        bars.forEachIndexed { i, h ->
            val x = s * (0.18f + i * 0.18f)
            drawRoundRect(Color(0xFF11111B),
                topLeft = Offset(x, s * (0.5f - h / 2f)),
                size = Size(s * 0.10f, s * h),
                cornerRadius = CornerRadius(s * 0.05f))
        }
    }
}

@Composable
fun IconBack(color: Color, modifier: Modifier = Modifier) = IconChevronLeft(color, modifier)

@Composable
fun IconPlay(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        val p = Path().apply {
            moveTo(s * 0.34f, s * 0.22f)
            lineTo(s * 0.78f, s * 0.5f)
            lineTo(s * 0.34f, s * 0.78f)
            close()
        }
        drawPath(p, color)
    }
}

@Composable
fun IconX(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.28f, s * 0.28f), Offset(s * 0.72f, s * 0.72f), s * 0.11f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.72f, s * 0.28f), Offset(s * 0.28f, s * 0.72f), s * 0.11f, StrokeCap.Round)
    }
}

@Composable
fun IconTrash(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.18f, s * 0.26f), Offset(s * 0.82f, s * 0.26f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.40f, s * 0.16f), Offset(s * 0.60f, s * 0.16f), s * 0.09f, StrokeCap.Round)
        val p = Path().apply {
            moveTo(s * 0.26f, s * 0.34f)
            lineTo(s * 0.32f, s * 0.84f)
            lineTo(s * 0.68f, s * 0.84f)
            lineTo(s * 0.74f, s * 0.34f)
        }
        drawPath(p, color, style = Stroke(width = s * 0.09f, cap = StrokeCap.Round))
        drawLine(color, Offset(s * 0.42f, s * 0.44f), Offset(s * 0.44f, s * 0.74f), s * 0.07f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.58f, s * 0.44f), Offset(s * 0.56f, s * 0.74f), s * 0.07f, StrokeCap.Round)
    }
}

@Composable
fun IconGear(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        val c = Offset(s * 0.5f, s * 0.5f)
        drawCircle(color, radius = s * 0.20f, center = c, style = Stroke(width = s * 0.10f))
        for (k in 0 until 8) {
            val a = Math.toRadians((k * 45).toDouble())
            val inner = Offset(c.x + (s * 0.26f) * kotlin.math.cos(a).toFloat(), c.y + (s * 0.26f) * kotlin.math.sin(a).toFloat())
            val outer = Offset(c.x + (s * 0.40f) * kotlin.math.cos(a).toFloat(), c.y + (s * 0.40f) * kotlin.math.sin(a).toFloat())
            drawLine(color, inner, outer, s * 0.10f, StrokeCap.Round)
        }
    }
}

@Composable
fun IconSearch(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawCircle(color, radius = s * 0.28f, center = Offset(s * 0.44f, s * 0.44f), style = Stroke(width = s * 0.09f))
        drawLine(color, Offset(s * 0.64f, s * 0.64f), Offset(s * 0.86f, s * 0.86f), s * 0.10f, StrokeCap.Round)
    }
}

@Composable
fun IconSkipBack(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.46f, s * 0.26f), Offset(s * 0.20f, s * 0.5f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.20f, s * 0.5f), Offset(s * 0.46f, s * 0.74f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.80f, s * 0.26f), Offset(s * 0.54f, s * 0.5f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.54f, s * 0.5f), Offset(s * 0.80f, s * 0.74f), s * 0.09f, StrokeCap.Round)
    }
}

@Composable
fun IconSkipForward(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.54f, s * 0.26f), Offset(s * 0.80f, s * 0.5f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.80f, s * 0.5f), Offset(s * 0.54f, s * 0.74f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.20f, s * 0.26f), Offset(s * 0.46f, s * 0.5f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.46f, s * 0.5f), Offset(s * 0.20f, s * 0.74f), s * 0.09f, StrokeCap.Round)
    }
}

@Composable
fun IconMore(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawCircle(color, radius = s * 0.07f, center = Offset(s * 0.5f, s * 0.22f))
        drawCircle(color, radius = s * 0.07f, center = Offset(s * 0.5f, s * 0.5f))
        drawCircle(color, radius = s * 0.07f, center = Offset(s * 0.5f, s * 0.78f))
    }
}

@Composable
fun IconCheck(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.20f, s * 0.52f), Offset(s * 0.42f, s * 0.74f), s * 0.11f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.42f, s * 0.74f), Offset(s * 0.82f, s * 0.28f), s * 0.11f, StrokeCap.Round)
    }
}

@Composable
fun IconFlag(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.24f, s * 0.14f), Offset(s * 0.24f, s * 0.88f), s * 0.09f, StrokeCap.Round)
        val p = Path().apply {
            moveTo(s * 0.24f, s * 0.18f)
            lineTo(s * 0.78f, s * 0.18f)
            lineTo(s * 0.66f, s * 0.36f)
            lineTo(s * 0.78f, s * 0.54f)
            lineTo(s * 0.24f, s * 0.54f)
            close()
        }
        drawPath(p, color)
    }
}

@Composable
fun IconWatch(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawCircle(color, radius = s * 0.26f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.09f))
        drawLine(color, Offset(s * 0.5f, s * 0.38f), Offset(s * 0.5f, s * 0.5f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.60f, s * 0.56f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.38f, s * 0.26f), Offset(s * 0.40f, s * 0.12f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.62f, s * 0.26f), Offset(s * 0.60f, s * 0.12f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.38f, s * 0.74f), Offset(s * 0.40f, s * 0.88f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.62f, s * 0.74f), Offset(s * 0.60f, s * 0.88f), s * 0.09f, StrokeCap.Round)
    }
}

@Composable
fun IconPhone(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawRoundRect(color, Offset(s * 0.30f, s * 0.10f), Size(s * 0.40f, s * 0.80f), CornerRadius(s * 0.08f), style = Stroke(width = s * 0.09f))
        drawLine(color, Offset(s * 0.44f, s * 0.78f), Offset(s * 0.56f, s * 0.78f), s * 0.08f, StrokeCap.Round)
    }
}

@Composable
fun IconCopy(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.09f
        drawRoundRect(color = color, topLeft = Offset(s * 0.30f, s * 0.30f), size = Size(s * 0.52f, s * 0.52f), cornerRadius = CornerRadius(s * 0.10f), style = Stroke(stroke))
        drawPath(
            path = Path().apply {
                moveTo(s * 0.62f, s * 0.22f); lineTo(s * 0.26f, s * 0.22f); lineTo(s * 0.26f, s * 0.60f)
            },
            color = color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
fun IconClock(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = this.size.minDimension
        drawCircle(color, radius = s * 0.36f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.09f))
        drawLine(color, Offset(s * 0.5f, s * 0.30f), Offset(s * 0.5f, s * 0.5f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.64f, s * 0.60f), s * 0.08f, StrokeCap.Round)
    }
}
