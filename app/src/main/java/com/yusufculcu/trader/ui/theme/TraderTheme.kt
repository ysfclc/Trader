package com.yusufculcu.trader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TraderColors = darkColorScheme(
    primary = Color(0xFFB9F36A), onPrimary = Color(0xFF18210D),
    secondary = Color(0xFF62D8C2), onSecondary = Color(0xFF06231E),
    background = Color(0xFF0B1012), onBackground = Color(0xFFE8EFEC),
    surface = Color(0xFF121A1D), onSurface = Color(0xFFE8EFEC),
    surfaceVariant = Color(0xFF202B2E), onSurfaceVariant = Color(0xFFB3C0BC),
    outline = Color(0xFF3A4948), error = Color(0xFFFF8A80)
)

@Composable
fun TraderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TraderColors, content = content)
}
