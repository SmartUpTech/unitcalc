package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.dimensionResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.timer.TimerDurationFormatter
import net.smartlogic.unitconverter.timer.model.TimerDraft

@Composable
fun TimerSettingSection(
    draft: TimerDraft,
    onDraftChange: (TimerDraft) -> Unit,
    onStart: () -> Unit,
) {
    val semantic = GraphyThemeTokens.semanticColors
    val spacing = GraphyThemeTokens.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = spacing.sm),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicTextField(
                value = draft.label,
                onValueChange = { value ->
                    onDraftChange(draft.copy(label = value.take(TimerDurationFormatter.MAX_LABEL_LENGTH)))
                },
                cursorBrush = SolidColor(semantic.primaryText),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = semantic.primaryText,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                ),
                singleLine = true,
                decorationBox = { inner ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.padding(vertical = spacing.xs)) {
                            if (draft.label.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.timer_label_hint),
                                    color = semantic.secondaryText,
                                    fontSize = 16.sp,
                                )
                            }
                            inner()
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .padding(top = dimensionResource(R.dimen.converter_mode_margin_bottom)),
                            color = semantic.connector,
                            thickness = dimensionResource(R.dimen.converter_divider_height),
                        )
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = spacing.sm),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimerWheelPicker(
                    value = draft.hours,
                    range = 0..TimerDurationFormatter.MAX_HOURS,
                    unitLabel = stringResource(R.string.timer_hours),
                    onValueChange = { onDraftChange(draft.copy(hours = it)) },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
                TimerWheelPicker(
                    value = draft.minutes,
                    range = 0..59,
                    unitLabel = stringResource(R.string.timer_min),
                    onValueChange = { onDraftChange(draft.copy(minutes = it)) },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
                TimerWheelPicker(
                    value = draft.seconds,
                    range = 0..59,
                    unitLabel = stringResource(R.string.timer_sec),
                    onValueChange = { onDraftChange(draft.copy(seconds = it)) },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }
        }

        TimerPlayButton(
            enabled = !draft.isZero,
            size = TimerIconButtonSize.Large,
            contentDescription = stringResource(R.string.timer_start),
            onClick = onStart,
            modifier = Modifier.padding(bottom = spacing.sm),
        )
    }
}
