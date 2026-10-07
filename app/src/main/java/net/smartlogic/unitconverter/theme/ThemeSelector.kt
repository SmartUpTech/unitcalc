package net.smartlogic.unitconverter.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens

@Composable
fun ThemeSelector(state: ThemeSelectorState, onThemeTapped: (String) -> Unit, onDismissDialog: () -> Unit) {
    val spacing = GraphyThemeTokens.spacing
    val initialSelection = remember { state.cards.indexOfFirst { it.selected }.coerceAtLeast(0) }
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = initialSelection)
    LazyRow(
        state = scroll,
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(state.cards, key = { it.theme.id() }) { card -> ThemeCard(card, onThemeTapped) }
    }
    if (state.lockedThemeNameRes != null) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(stringResource(R.string.theme_locked_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Text(stringResource(state.lockedThemeNameRes), style = MaterialTheme.typography.titleMedium)
                    Text(pluralStringResource(R.plurals.theme_locked_message, state.daysUntilTappedUnlock,
                        state.daysUntilTappedUnlock))
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissDialog) { Text(stringResource(R.string.theme_locked_dismiss)) }
            },
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
    val previewHeight = dimensionResource(R.dimen.theme_swatch_height)
    val iconSize = dimensionResource(R.dimen.theme_state_icon_size)
    // The label slot uses sp so every item grows equally with the user's font size.
    val labelHeight = dimensionResource(R.dimen.theme_card_label_height)
    val itemHeight = previewHeight + iconSize + labelHeight + spacing.xs * 2 + spacing.sm * 2
    Surface(
        modifier = Modifier
            .width(dimensionResource(R.dimen.theme_card_width))
            .height(itemHeight)
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = card.selected, role = Role.RadioButton, onClick = { onThemeTapped(theme.id()) })
            .semantics {
                contentDescription = name
                stateDescription = status
            },
        color = if (card.selected) GraphyThemeTokens.semanticColors.surfaceElevated else MaterialTheme.colorScheme.surface,
        contentColor = if (card.selected) GraphyThemeTokens.semanticColors.onConstant else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        // One accessible choice; swatch, icon and visible label are decorative children.
        Column(
            modifier = Modifier.padding(spacing.sm).clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(previewHeight)
                    .background(Color(theme.background()), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    ThemeColorDot(Color(theme.mainText()))
                    ThemeColorDot(Color(theme.functions()))
                    ThemeColorDot(Color(theme.equal()))
                }
            }
            // A small, borderless status line aligns names without an empty badge.
            Box(modifier = Modifier.fillMaxWidth().height(iconSize), contentAlignment = Alignment.Center) {
                if (!card.unlocked || card.selected) {
                    Icon(
                        painterResource(if (card.unlocked) R.drawable.ic_theme_check else R.drawable.ic_theme_lock),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.TopCenter) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ThemeColorDot(color: Color) {
    Box(Modifier.size(dimensionResource(R.dimen.theme_swatch_color_size)).background(color, CircleShape))
}
