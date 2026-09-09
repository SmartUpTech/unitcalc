package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens

enum class TimerIconButtonSize(val diameter: Dp, val iconSize: Dp) {
    Large(72.dp, 32.dp),
    Medium(56.dp, 24.dp),
    Small(40.dp, 18.dp),
}

@Composable
fun TimerPlayButton(
    enabled: Boolean,
    size: TimerIconButtonSize,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TimerCircleIconButton(
        background = GraphyThemeTokens.semanticColors.equal,
        iconRes = R.drawable.ic_timer_play,
        iconTint = Color.White,
        enabled = enabled,
        size = size,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun TimerPauseButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TimerCircleIconButton(
        background = GraphyThemeTokens.semanticColors.equal,
        iconRes = R.drawable.ic_timer_pause,
        iconTint = Color.White,
        enabled = true,
        size = TimerIconButtonSize.Medium,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun TimerResetButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TimerCircleIconButton(
        background = GraphyThemeTokens.semanticColors.connector.copy(alpha = 0.25f),
        iconRes = R.drawable.ic_timer_reset,
        iconTint = GraphyThemeTokens.semanticColors.primaryText,
        enabled = true,
        size = TimerIconButtonSize.Medium,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun TimerCircleIconButton(
    background: Color,
    iconRes: Int,
    iconTint: Color,
    enabled: Boolean,
    size: TimerIconButtonSize,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size.diameter)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size.iconSize),
        )
    }
}
