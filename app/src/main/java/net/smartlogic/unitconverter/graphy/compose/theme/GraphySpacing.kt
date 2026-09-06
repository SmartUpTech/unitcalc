package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GraphySpacing(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
)

val GraphySpacingDefault = GraphySpacing(
    xs = 4.dp,
    sm = 8.dp,
    md = 16.dp,
    lg = 24.dp,
)

val LocalGraphySpacing = staticCompositionLocalOf { GraphySpacingDefault }
