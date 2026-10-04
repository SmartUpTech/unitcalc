package net.smartlogic.unitconverter.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.graphy.theme.GraphySemanticStyle

@Composable
fun ThemeSelector(state: ThemeSelectorState, onThemeTapped: (String) -> Unit, onDismissDialog: () -> Unit) {
    val spacing = GraphyThemeTokens.spacing
    val initialSelection = remember { state.cards.indexOfFirst { it.selected }.coerceAtLeast(0) }
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = initialSelection)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        LazyRow(state = scroll, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(state.cards, key = { it.theme.id() }) { card -> ThemeCard(card, onThemeTapped) }
        }
        if (state.nextThemeNameRes == null) {
            Text(stringResource(R.string.theme_all_unlocked), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                pluralStringResource(R.plurals.theme_next_unlock, state.daysUntilNextUnlock,
                    stringResource(state.nextThemeNameRes), state.daysUntilNextUnlock),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.theme_unlock_progress, state.progress, state.cycleDays),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (state.lockedThemeNameRes != null) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(stringResource(R.string.theme_locked_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(stringResource(state.lockedThemeNameRes), style = MaterialTheme.typography.titleMedium)
                    Text(pluralStringResource(R.plurals.theme_locked_message, state.daysUntilTappedUnlock,
                        state.daysUntilTappedUnlock))
                    Text(stringResource(R.string.theme_unlock_progress, state.progress, state.cycleDays))
                }
            },
            confirmButton = { TextButton(onClick = onDismissDialog) { Text(stringResource(android.R.string.ok)) } },
        )
    }
}

@Composable
private fun ThemeCard(card: ThemeCardState, onThemeTapped: (String) -> Unit) {
    val theme = card.theme
    val spacing = GraphyThemeTokens.spacing
    val name = stringResource(theme.nameRes())
    val status = stringResource(when {
        !card.unlocked -> R.string.theme_state_locked
        card.selected -> R.string.theme_state_selected
        else -> R.string.theme_state_available
    })
    val ink = Color(GraphySemanticStyle.readable(theme.mainText(), theme.background()))
    Surface(
        onClick = { onThemeTapped(theme.id()) },
        modifier = Modifier.width(dimensionResource(R.dimen.theme_card_width)).semantics {
            contentDescription = name
            stateDescription = status
            selected = card.selected
        },
        color = Color(theme.background()),
        contentColor = ink,
        shape = MaterialTheme.shapes.medium,
        border = if (card.selected) BorderStroke(dimensionResource(R.dimen.theme_selection_stroke), ink) else null,
    ) {
        // One accessible card: the miniature calculator and state icon are decorative.
        Column(
            modifier = Modifier.padding(spacing.md).clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(stringResource(R.string.theme_preview_result), style = MaterialTheme.typography.titleMedium)
            PreviewRow(R.string.clear, R.string.divide, Color(GraphySemanticStyle.connector(theme)),
                Color(theme.operatorContentColor()))
            PreviewRow(R.string.seven, R.string.multiply, Color(theme.mainText()), Color(theme.operatorContentColor()))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.eight), color = Color(theme.mainText()), modifier = Modifier.weight(1f))
                Surface(color = Color(theme.equal()), contentColor = Color(GraphySemanticStyle.readable(theme.background(), theme.equal())),
                    shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.equal), modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs))
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                if (!card.unlocked || card.selected) {
                    Spacer(Modifier.width(spacing.xs))
                    Icon(
                        painterResource(if (card.unlocked) R.drawable.ic_theme_check else R.drawable.ic_theme_lock),
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.theme_state_icon_size)),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(left: Int, right: Int, leftColor: Color, rightColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(left), color = leftColor, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(right), color = rightColor, style = MaterialTheme.typography.bodyMedium)
    }
}
