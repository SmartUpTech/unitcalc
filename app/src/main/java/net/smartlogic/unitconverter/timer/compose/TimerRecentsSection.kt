package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.timer.TimerDurationFormatter
import net.smartlogic.unitconverter.timer.model.RecentTimer

@Composable
fun TimerRecentsSection(
    recents: List<RecentTimer>,
    onPlayRecent: (RecentTimer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = GraphyThemeTokens.semanticColors
    val spacing = GraphyThemeTokens.spacing
    val defaultLabel = stringResource(R.string.timer_default_label)

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.timer_recent).uppercase(),
            color = semantic.secondaryText,
            fontSize = 12.sp,
            letterSpacing = 0.06.sp,
            modifier = Modifier.padding(bottom = spacing.sm),
        )
        if (recents.isEmpty()) {
            Text(
                text = stringResource(R.string.timer_default_label),
                color = semantic.secondaryText,
                fontSize = 14.sp,
            )
        } else {
            recents.forEachIndexed { index, recent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = TimerDurationFormatter.displayLabel(recent.label, defaultLabel),
                            color = semantic.primaryText,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = TimerDurationFormatter.formatDuration(recent.durationMs),
                            color = semantic.secondaryText,
                            fontSize = 14.sp,
                        )
                    }
                    TimerPlayButton(
                        enabled = true,
                        size = TimerIconButtonSize.Small,
                        contentDescription = stringResource(R.string.timer_start),
                        onClick = { onPlayRecent(recent) },
                    )
                }
                if (index < recents.lastIndex) {
                    HorizontalDivider(color = semantic.connector.copy(alpha = 0.25f))
                }
            }
        }
    }
}
