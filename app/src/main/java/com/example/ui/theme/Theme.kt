package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = Color.White,
    secondary = MidnightSecondary,
    background = MidnightBackground,
    surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB3B3B3),
    error = RedError
)

private val NeonColorScheme = darkColorScheme(
    primary = NeonPrimary,
    onPrimary = Color.Black,
    secondary = NeonSecondary,
    background = NeonBackground,
    surface = NeonSurface,
    surfaceVariant = NeonSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFA5C5D9),
    error = RedError
)

private val OledColorScheme = darkColorScheme(
    primary = OledPrimary,
    onPrimary = Color.White,
    secondary = OledSecondary,
    background = OledBackground,
    surface = OledSurface,
    surfaceVariant = OledSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF9E9E9E),
    error = RedError
)

private val TealColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.Black,
    secondary = TealSecondary,
    background = TealBackground,
    surface = TealSurface,
    surfaceVariant = TealSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF90D9D1),
    error = RedError
)

@Composable
fun VidoraTheme(
    themeId: String = "midnight",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeId) {
        "neon" -> NeonColorScheme
        "oled" -> OledColorScheme
        "teal" -> TealColorScheme
        else -> MidnightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
