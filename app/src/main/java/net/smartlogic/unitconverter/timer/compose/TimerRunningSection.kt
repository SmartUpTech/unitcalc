package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.timer.TimerDurationFormatter

@Composable
fun TimerRunningSection(
    label: String,
    remainingMs: Long,
    originalDurationMs: Long,
    paused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onRestart: () -> Unit,
) {
    val semantic = GraphyThemeTokens.semanticColors
    val spacing = GraphyThemeTokens.spacing
    val displayLabel = TimerDurationFormatter.displayLabel(label, stringResource(R.string.timer_default_label))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = displayLabel.uppercase(),
                color = semantic.secondaryText,
                fontSize = 13.sp,
                letterSpacing = 0.08.sp,
            )
            Box(
                modifier = Modifier
                    .padding(vertical = spacing.md)
                    .size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                TimerProgressRing(
                    remainingMs = remainingMs,
                    totalMs = originalDurationMs,
                    modifier = Modifier.matchParentSize(),
                )
                Text(
                    text = TimerDurationFormatter.formatCountdown(remainingMs),
                    color = semantic.primaryText,
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (paused) {
                TimerPlayButton(
                    enabled = true,
                    size = TimerIconButtonSize.Medium,
                    contentDescription = stringResource(R.string.timer_resume),
                    onClick = onResume,
                )
            } else {
                TimerPauseButton(
                    contentDescription = stringResource(R.string.timer_pause),
                    onClick = onPause,
                )
            }
            TimerResetButton(
                contentDescription = stringResource(R.string.timer_reset),
                onClick = onReset,
                modifier = Modifier.padding(start = spacing.lg),
            )
        }
    }
}

@Composable
fun TimerCompletedSection(
    label: String,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
) {
    val semantic = GraphyThemeTokens.semanticColors
    val spacing = GraphyThemeTokens.spacing
    val displayLabel = TimerDurationFormatter.displayLabel(label, stringResource(R.string.timer_default_label))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayLabel.uppercase(),
                color = semantic.secondaryText,
                fontSize = 13.sp,
            )
            Text(
                text = TimerDurationFormatter.formatCountdown(0L),
                color = semantic.primaryText,
                fontSize = 40.sp,
                modifier = Modifier.padding(vertical = spacing.md),
            )
            Text(
                text = stringResource(R.string.timer_times_up),
                color = semantic.equal,
                fontSize = 18.sp,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            modifier = Modifier.padding(bottom = spacing.sm),
        ) {
            TimerResetButton(
                contentDescription = stringResource(R.string.timer_dismiss),
                onClick = onDismiss,
            )
            TimerPlayButton(
                enabled = true,
                size = TimerIconButtonSize.Medium,
                contentDescription = stringResource(R.string.timer_restart),
                onClick = onRestart,
            )
        }
    }
}
