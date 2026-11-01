package net.smartlogic.unitconverter.timer.compose

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.timer.TimerDurationFormatter
import java.util.Date

@Composable
fun TimerRunningSection(
    label: String, remainingMs: Long, originalDurationMs: Long, paused: Boolean,
    onPause: () -> Unit, onResume: () -> Unit, onReset: () -> Unit, onRestart: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val gap = dimensionResource(R.dimen.timer_compact_gap)
    val timeSize = with(LocalDensity.current) { dimensionResource(R.dimen.timer_countdown_size).toSp() }
    val end = DateFormat.getTimeFormat(LocalContext.current).format(Date(System.currentTimeMillis() + remainingMs))
    val status = if (paused) stringResource(R.string.timer_paused) else stringResource(R.string.timer_ends_at, end)
    val touch = dimensionResource(R.dimen.converter_touch_target)
    val wideAt = dimensionResource(R.dimen.timer_wide_breakpoint)
    val textMeasurer = rememberTextMeasurer()
    val countdown = TimerDurationFormatter.formatCountdown(remainingMs)
    val countdownStyles = listOf(MaterialTheme.typography.displaySmall.copy(fontSize = timeSize),
        MaterialTheme.typography.headlineLarge, MaterialTheme.typography.headlineMedium,
        MaterialTheme.typography.headlineSmall)
    @Composable fun display(modifier: Modifier) {
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            val diameter = minOf(maxWidth, maxHeight, dimensionResource(R.dimen.timer_ring_size))
            val countdownStyle = countdownStyles.firstOrNull {
                textMeasurer.measure(countdown, style = it, softWrap = false).size.width <= constraints.maxWidth
            }
            if (countdownStyle == null) {
                Text(stringResource(R.string.tool_more_space), color = colors.onSurface)
                return@BoxWithConstraints
            }
            TimerProgressRing(remainingMs, originalDurationMs, Modifier.size(diameter))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(TimerDurationFormatter.displayLabel(label, stringResource(R.string.timer_default_label)),
                    style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(countdown, modifier = Modifier.fillMaxWidth(),
                    style = countdownStyle,
                    color = colors.onSurface, textAlign = TextAlign.Center, maxLines = 1)
                Text(status, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
    @Composable fun actions(modifier: Modifier) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically)) {
            TimerActionButton(stringResource(if (paused) R.string.timer_resume else R.string.timer_pause),
                if (paused) onResume else onPause, Modifier.fillMaxWidth(), primary = true,
                text = stringResource(if (paused) R.string.timer_action_resume else R.string.timer_action_pause))
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                TimerActionButton(stringResource(R.string.timer_reset), onReset, Modifier.weight(1f), text = stringResource(R.string.timer_action_reset))
                TimerActionButton(stringResource(R.string.timer_restart), onRestart, Modifier.weight(1f), text = stringResource(R.string.timer_action_restart))
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= wideAt && maxHeight < wideAt
        val displayMin = with(LocalDensity.current) { timeSize.toDp() * 2 + MaterialTheme.typography.bodyLarge.fontSize.toDp() * 3 }
        val minHeight = if (wide) maxOf(displayMin, touch * 2 + gap) else displayMin + touch * 2 + gap
        if (maxHeight < minHeight) {
            Text(stringResource(R.string.tool_more_space), Modifier.align(Alignment.Center), color = colors.onSurface)
        } else if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap * 2)) {
                display(Modifier.weight(1f).fillMaxHeight())
                actions(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                display(Modifier.fillMaxWidth().weight(1f))
                actions(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun TimerCompletedSection(label: String, onDismiss: () -> Unit, onRestart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val gap = dimensionResource(R.dimen.timer_compact_gap)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(TimerDurationFormatter.displayLabel(label, stringResource(R.string.timer_default_label)),
                style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.timer_times_up), style = MaterialTheme.typography.headlineLarge,
                color = colors.primary, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            TimerActionButton(stringResource(R.string.timer_dismiss), onDismiss, Modifier.weight(1f), primary = true, text = stringResource(R.string.timer_action_dismiss))
            TimerActionButton(stringResource(R.string.timer_restart), onRestart, Modifier.weight(1f), text = stringResource(R.string.timer_action_restart))
        }
    }
}
