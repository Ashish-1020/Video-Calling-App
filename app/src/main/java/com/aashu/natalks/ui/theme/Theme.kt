package com.aashu.natalks.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BlueTealColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = OnAccent,
    primaryContainer = AccentContainer,
    onPrimaryContainer = TextDeep,
    secondary = AccentTeal,
    onSecondary = TextDeep,
    background = BackgroundLight,
    onBackground = TextDeep,
    surface = SurfaceWhite,
    onSurface = TextDeep,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = TextMuted,
    error = DangerRed,
    onError = OnAccent
)

@Composable
fun NaTalksTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlueTealColorScheme,
        typography = NaTalksTypography,
        shapes = NaTalksShapes,
        content = content
    )
}
