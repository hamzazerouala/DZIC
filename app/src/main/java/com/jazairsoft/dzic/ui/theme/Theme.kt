package com.jazairsoft.dzic.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = DzicGreen,
    onPrimary = DzicNight,
    secondary = DzicSand,
    onSecondary = DzicNight,
    background = DzicNight,
    onBackground = DzicOnDark,
    surface = DzicNight,
    onSurface = DzicOnDark,
    surfaceVariant = DzicNightElevated,
    onSurfaceVariant = DzicMutedDark,
    outline = DzicMutedDark
)

private val LightColors = lightColorScheme(
    primary = DzicGreenDark,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = DzicSand,
    background = DzicSurfaceLight,
    onBackground = DzicNight,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = DzicNight,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE9EEF6),
    onSurfaceVariant = DzicMutedLight,
    outline = DzicMutedLight
)


@Composable
fun DzicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = DzicTypography,
        content = content
    )
}
