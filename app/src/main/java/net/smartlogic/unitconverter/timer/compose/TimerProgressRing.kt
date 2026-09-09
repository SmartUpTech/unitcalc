package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens

@Composable
fun TimerProgressRing(
    remainingMs: Long,
    totalMs: Long,
    modifier: Modifier = Modifier,
) {
    val semantic = GraphyThemeTokens.semanticColors
    val progress = if (totalMs <= 0L) {
        0f
    } else {
        (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    }

    Canvas(modifier = modifier) {
        val stroke = 3.dp.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = semantic.connector.copy(alpha = 0.35f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = semantic.equal,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
