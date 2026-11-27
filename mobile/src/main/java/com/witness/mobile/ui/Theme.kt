package com.witness.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.witness.core.theme.Catppuccin

val WitnessColors = darkColorScheme(
    primary = Catppuccin.Lavender,
    onPrimary = Catppuccin.Crust,
    primaryContainer = Catppuccin.Surface0,
    onPrimaryContainer = Catppuccin.Lavender,
    secondary = Catppuccin.Blue,
    onSecondary = Catppuccin.Crust,
    tertiary = Catppuccin.Mauve,
    background = Catppuccin.Base,
    onBackground = Catppuccin.Text,
    surface = Catppuccin.Mantle,
    onSurface = Catppuccin.Text,
    surfaceVariant = Catppuccin.Surface0,
    onSurfaceVariant = Catppuccin.Subtext0,
    outline = Catppuccin.Surface1,
    error = Catppuccin.Red,
    onError = Catppuccin.Crust,
)

private val Mono = FontFamily.Monospace

val WitnessTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, color = Catppuccin.Overlay1),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

/** Моно-стиль для таймкодов/длительностей (как JetBrains Mono в макете). */
val MonoTime = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = Catppuccin.Overlay1)
val MonoTimeLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-1).sp, color = Catppuccin.Text)
val MonoMeta = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = Catppuccin.Overlay0)

@Composable
fun WitnessTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WitnessColors, typography = WitnessTypography, content = content)
}