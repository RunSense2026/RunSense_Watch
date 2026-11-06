package com.bmb.runsensewear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/**
 * 워치 전용 다크 컬러 스킴
 */
private val RunSenseWearColors = Colors(
    primary = RunSenseColors.Purple300,
    primaryVariant = RunSenseColors.Purple700,
    secondary = RunSenseColors.Blue300,
    secondaryVariant = RunSenseColors.Blue600,
    background = RunSenseColors.BgGradientStart,
    surface = RunSenseColors.SurfaceSolid,
    error = RunSenseColors.Bad,
    onPrimary = RunSenseColors.TextPrimary,
    onSecondary = RunSenseColors.TextPrimary,
    onBackground = RunSenseColors.TextPrimary,
    onSurface = RunSenseColors.TextPrimary,
    onError = RunSenseColors.TextPrimary
)

@Composable
fun RunSenseWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = RunSenseWearColors,
        typography = RunSenseTypography,
        shapes = RunSenseShapes,
        content = content
    )
}
