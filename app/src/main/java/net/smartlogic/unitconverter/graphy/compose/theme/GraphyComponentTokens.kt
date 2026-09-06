package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GraphyComponentTokens(
    val nodeMinWidth: Dp,
    val nodeMinHeight: Dp,
    val nodeCornerRadius: Dp,
    val operationMarkerSize: Dp,
    val connectorStroke: Dp,
)

val GraphyComponentTokensDefault = GraphyComponentTokens(
    nodeMinWidth = 72.dp,
    nodeMinHeight = 40.dp,
    nodeCornerRadius = 6.dp,
    operationMarkerSize = 28.dp,
    connectorStroke = 1.5.dp,
)

val LocalGraphyComponentTokens = staticCompositionLocalOf { GraphyComponentTokensDefault }
