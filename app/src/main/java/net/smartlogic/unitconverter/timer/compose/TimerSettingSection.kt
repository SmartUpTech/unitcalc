package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.timer.TimerDurationFormatter
import net.smartlogic.unitconverter.timer.model.TimerDraft

@Composable
fun TimerSettingSection(draft: TimerDraft, onDraftChange: (TimerDraft) -> Unit, onStart: () -> Unit) {
    val gap = dimensionResource(R.dimen.timer_compact_gap)
    val touch = dimensionResource(R.dimen.converter_touch_target)
    val textHeight = with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.fontSize.toDp() * 1.5f }
    val rowHeight = maxOf(touch, textHeight + gap * 2)
    val wheelHeight = with(LocalDensity.current) {
        maxOf(touch, MaterialTheme.typography.headlineSmall.lineHeight.toDp()) +
            MaterialTheme.typography.bodySmall.fontSize.toDp() * 1.5f
    }
    val wideAt = dimensionResource(R.dimen.timer_wide_breakpoint)
    var editLabel by rememberSaveable { mutableStateOf(false) }
    var labelInput by rememberSaveable { mutableStateOf("") }

    @Composable fun label() {
        TextButton(onClick = { labelInput = draft.label; editLabel = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = rowHeight)) {
            Text(draft.label.ifEmpty { stringResource(R.string.timer_label_hint) },
                style = MaterialTheme.typography.bodyLarge,
                color = GraphyThemeTokens.semanticColors.secondaryText)
        }
    }
    @Composable fun wheels(modifier: Modifier) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            TimerWheelPicker(draft.hours, 0..TimerDurationFormatter.MAX_HOURS,
                stringResource(R.string.timer_hours), { onDraftChange(draft.copy(hours = it)) }, Modifier.weight(1f).fillMaxHeight())
            TimerWheelPicker(draft.minutes, 0..59, stringResource(R.string.timer_min),
                { onDraftChange(draft.copy(minutes = it)) }, Modifier.weight(1f).fillMaxHeight())
            TimerWheelPicker(draft.seconds, 0..59, stringResource(R.string.timer_sec),
                { onDraftChange(draft.copy(seconds = it)) }, Modifier.weight(1f).fillMaxHeight())
        }
    }
    @Composable fun presets() {
        val presets = LocalContext.current.resources.getIntArray(R.array.timer_preset_minutes)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            presets.toList().chunked(3).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    group.forEach { minutes ->
                        val selected = draft.durationMs == minutes * 60_000L
                        TimerActionButton(stringResource(R.string.timer_preset_minutes, minutes),
                            onClick = { onDraftChange(draft.copy(hours = 0, minutes = minutes, seconds = 0)) },
                            modifier = Modifier.weight(1f).semantics { this.selected = selected }, primary = selected)
                    }
                }
            }
        }
    }
    @Composable fun start() {
        TimerActionButton(stringResource(R.string.timer_start), onStart,
            Modifier.fillMaxWidth(), primary = true, enabled = !draft.isZero)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= wideAt && maxHeight < wideAt
        val minimum = if (wide) maxOf(rowHeight + wheelHeight, rowHeight * 3 + gap * 2)
            else rowHeight * 4 + wheelHeight + gap * 4
        if (maxHeight < minimum) {
            Text(stringResource(R.string.tool_more_space), modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurface)
        } else if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap * 2)) {
                Column(Modifier.weight(1f)) { label(); wheels(Modifier.fillMaxWidth().weight(1f)) }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically)) {
                    presets(); start()
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                label()
                wheels(Modifier.fillMaxWidth().weight(1f))
                presets()
                start()
            }
        }
    }
    if (editLabel) {
        fun save() { onDraftChange(draft.copy(label = labelInput)); editLabel = false }
        AlertDialog(onDismissRequest = { editLabel = false },
            title = { Text(stringResource(R.string.timer_label_hint)) },
            text = {
                OutlinedTextField(labelInput, { labelInput = it.take(TimerDurationFormatter.MAX_LABEL_LENGTH) },
                    singleLine = true, label = { Text(stringResource(R.string.timer_label_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }))
            },
            confirmButton = { TextButton(onClick = { save() }) { Text(stringResource(R.string.timer_label_save)) } },
            dismissButton = { TextButton(onClick = { editLabel = false }) { Text(stringResource(android.R.string.cancel)) } })
    }
}

