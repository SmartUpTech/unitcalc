package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class GraphyShapeTokens(
    val node: RoundedCornerShape,
    val card: RoundedCornerShape,
    val chip: RoundedCornerShape,
)

val GraphyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

val GraphyShapeTokensDefault = GraphyShapeTokens(
    node = RoundedCornerShape(12.dp),
    card = RoundedCornerShape(12.dp),
    chip = RoundedCornerShape(16.dp),
)

val LocalGraphyShapeTokens = staticCompositionLocalOf { GraphyShapeTokensDefault }
