package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color.White,
    primaryContainer = LimeGreen,
    onPrimaryContainer = DarkOlive,
    secondary = WarmCard,
    onSecondary = DarkOlive,
    secondaryContainer = CreamCard,
    onSecondaryContainer = DarkText,
    background = BgWarm,
    onBackground = DarkText,
    surface = Color.White,
    onSurface = DarkText,
    surfaceVariant = WarmLight,
    onSurfaceVariant = TextGray,
    outline = SandyBorder
)

// Dark schema is also warm-toned to maintain the earth "Natural Tones" feel even in low light
private val DarkColorScheme = darkColorScheme(
    primary = LimeGreen,
    onPrimary = DarkOlive,
    primaryContainer = DeepGreen,
    onPrimaryContainer = Color.White,
    secondary = CreamCard,
    onSecondary = DarkOlive,
    secondaryContainer = DarkOlive,
    onSecondaryContainer = CreamCard,
    background = DarkOlive,
    onBackground = BgWarm,
    surface = Color(0xFF242616),
    onSurface = BgWarm,
    surfaceVariant = Color(0xFF3B3E25),
    onSurfaceVariant = GrayBorder,
    outline = SandyBorder
)

@Composable
fun PolyglotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
