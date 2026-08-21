package com.khamrnet.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF102A43),
    onSecondary = Color.White,
    tertiary = Color(0xFFD99A2B),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onSurface = Color(0xFF1E293B)
)

@Composable
fun KhamrTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
