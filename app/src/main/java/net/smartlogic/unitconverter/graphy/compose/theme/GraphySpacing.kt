package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

@Immutable
data class GraphySpacing(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
)

val LocalGraphySpacing = staticCompositionLocalOf<GraphySpacing> {
    error("GraphyTheme must provide spacing from Android resources")
}
