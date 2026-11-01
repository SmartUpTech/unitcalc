package net.smartlogic.unitconverter.timer.compose

import net.smartlogic.unitconverter.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.dimensionResource
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun TimerProgressRing(
    remainingMs: Long,
    totalMs: Long,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val progress = if (totalMs <= 0L) {
        0f
    } else {
        (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    }

    val strokeWidth = dimensionResource(R.dimen.spacing_3dp)
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = colors.outline.copy(alpha = 0.35f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = colors.primary,
            startAngle = 135f,
            sweepAngle = 270f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
