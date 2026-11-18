package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val PlusCutColorScheme =
  darkColorScheme(
    primary = NeonCyan,
    onPrimary = StudioBg,
    primaryContainer = StudioSurfaceElevated,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = TextPrimary,
    secondaryContainer = NeonPurpleSoft,
    onSecondaryContainer = TextPrimary,
    tertiary = AudioTeal,
    onTertiary = StudioBg,
    background = StudioBg,
    onBackground = TextPrimary,
    surface = StudioPanel,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    outlineVariant = StudioBorderSubtle,
    error = PlayheadCrimson,
    onError = TextPrimary,
  )

private val PlusCutShapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
  )

@Composable
fun PlusCutTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = PlusCutColorScheme,
    typography = PlusCutTypography,
    shapes = PlusCutShapes,
    content = content,
  )
}
