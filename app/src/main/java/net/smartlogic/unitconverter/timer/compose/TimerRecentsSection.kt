package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
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
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val page = selected.coerceIn(0, (recents.size - 1).coerceAtLeast(0))

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
            val recent = recents[page]
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selected = page - 1 }, enabled = page > 0) {
                    Icon(painterResource(R.drawable.ic_chevron_down),
                        contentDescription = stringResource(R.string.tool_previous),
                        modifier = Modifier.rotate(90f))
                }
                Text(stringResource(R.string.tool_page, page + 1, recents.size), modifier = Modifier.weight(1f),
                    color = semantic.secondaryText)
                IconButton(onClick = { selected = page + 1 }, enabled = page < recents.lastIndex) {
                    Icon(painterResource(R.drawable.ic_chevron_down),
                        contentDescription = stringResource(R.string.tool_next),
                        modifier = Modifier.rotate(-90f))
                }
            }
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
        }
    }
}
