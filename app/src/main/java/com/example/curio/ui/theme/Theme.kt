package com.example.curio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun CurioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val curioColors = if (darkTheme) DarkCurioColors else LightCurioColors

    CompositionLocalProvider(LocalCurioColors provides curioColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CurioTypography,
            shapes = CurioShapes,
            content = content
        )
    }
}

object CurioTheme {
    val colors: CurioColors
        @Composable
        get() = LocalCurioColors.current
}
