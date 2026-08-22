package com.khamrnet.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object KhamrColors {
    val PrimaryTeal = Color(0xFF0F766E)
    val DarkTeal = Color(0xFF0D635C)
    val LightTealBg = Color(0xFFF0FDFA)
    val TealBorder = Color(0xFF99F6E4)
    val TealText = Color(0xFF0F766E)

    val EmeraldSuccess = Color(0xFF059669)
    val EmeraldDark = Color(0xFF047857)
    val EmeraldBg = Color(0xFFECFDF5)
    val EmeraldBorder = Color(0xFFA7F3D0)

    val SlateDark = Color(0xFF0F172A)
    val SlateMedium = Color(0xFF334155)
    val SlateMuted = Color(0xFF64748B)
    val SlateLight = Color(0xFF94A3B8)
    val SlateBorder = Color(0xFFE2E8F0)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color.White

    val RoseDanger = Color(0xFFE11D48)
    val RoseBg = Color(0xFFFFF1F2)
    val RoseBorder = Color(0xFFFECDD3)

    val AmberWarning = Color(0xFFD97706)
    val AmberBg = Color(0xFFFFFBEB)
    val AmberBorder = Color(0xFFFDE68A)

    val BlueAccent = Color(0xFF2563EB)
    val BlueBg = Color(0xFFEFF6FF)
    val BlueBorder = Color(0xFFBFDBFE)

    val PurpleAccent = Color(0xFF7C3AED)
    val PurpleBg = Color(0xFFFAF5FF)
    val PurpleBorder = Color(0xFFE9D5FF)
}

private val LightColorScheme = lightColorScheme(
    primary = KhamrColors.PrimaryTeal,
    onPrimary = Color.White,
    secondary = KhamrColors.EmeraldSuccess,
    onSecondary = Color.White,
    tertiary = KhamrColors.AmberWarning,
    background = KhamrColors.Background,
    surface = KhamrColors.Surface,
    onSurface = KhamrColors.SlateDark,
    outline = KhamrColors.SlateBorder
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
