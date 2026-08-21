package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryBlue,
        onPrimary = BackgroundLight,
        primaryContainer = PrimaryIndigo,
        onPrimaryContainer = TextPrimaryDark,
        secondary = SecondaryTeal,
        onSecondary = BackgroundDark,
        background = BackgroundDark,
        onBackground = TextPrimaryDark,
        surface = SurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = TextSecondaryDark,
        outline = BorderDark,
        error = ErrorRed,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryBlueDark,
        onPrimary = SurfaceLight,
        primaryContainer = PrimaryBlue,
        onPrimaryContainer = SurfaceLight,
        secondary = SecondaryTeal,
        onSecondary = SurfaceLight,
        background = BackgroundLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = TextSecondaryLight,
        outline = BorderLight,
        error = ErrorRed,
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
  )
}

