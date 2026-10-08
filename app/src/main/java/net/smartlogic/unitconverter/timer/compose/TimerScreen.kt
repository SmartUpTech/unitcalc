package net.smartlogic.unitconverter.timer.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.timer.TimerViewModel
import net.smartlogic.unitconverter.timer.model.TimerPhase

@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = GraphyThemeTokens.spacing
    val semantic = GraphyThemeTokens.semanticColors
    var showRecents by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.md),
    ) {
        val minimumHeight = dimensionResource(R.dimen.converter_touch_target) * 3 +
            with(LocalDensity.current) { 88.sp.toDp() } + spacing.md * 4
        if (maxHeight < minimumHeight || maxWidth < dimensionResource(R.dimen.calculation_pane_min_width)) {
            Text(stringResource(R.string.tool_more_space), color = semantic.primaryText,
                modifier = Modifier.align(Alignment.Center))
            return@BoxWithConstraints
        }
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = uiState.phase,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "timerPhase",
            ) { phase ->
                when (phase) {
                    is TimerPhase.Setting -> TimerSettingSection(
                        draft = phase.draft,
                        onDraftChange = viewModel::updateDraft,
                        onStart = viewModel::startFromDraft,
                    )
                    is TimerPhase.Running -> TimerRunningSection(
                        label = phase.label,
                        remainingMs = uiState.remainingMs,
                        originalDurationMs = phase.originalDurationMs,
                        paused = false,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onReset = viewModel::reset,
                        onRestart = viewModel::restart,
                    )
                    is TimerPhase.Paused -> TimerRunningSection(
                        label = phase.label,
                        remainingMs = uiState.remainingMs,
                        originalDurationMs = phase.originalDurationMs,
                        paused = true,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onReset = viewModel::reset,
                        onRestart = viewModel::restart,
                    )
                    is TimerPhase.Completed -> TimerCompletedSection(
                        label = phase.label,
                        onDismiss = viewModel::dismissCompleted,
                        onRestart = viewModel::restart,
                    )
                }
            }

            HorizontalDivider(color = semantic.connector.copy(alpha = 0.35f))

            TextButton(onClick = { showRecents = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.timer_recent))
            }
        }
    }

    if (showRecents) {
        AlertDialog(
            onDismissRequest = { showRecents = false },
            text = {
                TimerRecentsSection(recents = uiState.recents, onPlayRecent = {
                    showRecents = false
                    viewModel.startRecent(it)
                })
            },
            confirmButton = {
                TextButton(onClick = { showRecents = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    uiState.pendingRecent?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::dismissReplaceRecent,
            title = { Text(stringResource(R.string.timer_replace_title)) },
            text = { Text(stringResource(R.string.timer_replace_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReplaceRecent) {
                    Text(stringResource(R.string.timer_replace_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissReplaceRecent) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
