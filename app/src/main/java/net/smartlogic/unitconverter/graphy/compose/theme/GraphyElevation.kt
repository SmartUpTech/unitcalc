package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GraphyElevation(
    val card: Dp,
    val node: Dp,
    val none: Dp,
)

val GraphyElevationDefault = GraphyElevation(
    card = 0.dp,
    node = 0.dp,
    none = 0.dp,
)

val LocalGraphyElevation = staticCompositionLocalOf { GraphyElevationDefault }
