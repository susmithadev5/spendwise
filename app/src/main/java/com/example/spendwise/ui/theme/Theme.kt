package com.example.spendwise.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SpendGreen,
    onPrimary = Color.White,
    background = NeutralBackground,
    onBackground = NeutralOnBackground,
    surface = Color.White,
    onSurface = NeutralOnBackground
)

private val DarkColors = darkColorScheme(
    primary = SpendGreen,
    onPrimary = Color.White,
    background = Color(0xFF0F1C11),
    onBackground = Color(0xFFE6EAE4),
    surface = Color(0xFF1B2A1D),
    onSurface = Color(0xFFE6EAE4)
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
