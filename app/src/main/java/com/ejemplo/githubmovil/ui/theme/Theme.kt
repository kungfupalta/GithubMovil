package com.ejemplo.githubmovil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface
)

@Composable
fun GithubMovilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
