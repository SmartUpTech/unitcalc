package net.smartlogic.unitconverter.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens

@Composable
fun ThemeSelector(
    state: ThemeSelectorState, onThemeTapped: (String) -> Unit, onDismissDialog: () -> Unit,
    onWatchAd: () -> Unit = {},
) {
    val spacing = GraphyThemeTokens.spacing
    val initialSelection = remember { state.cards.indexOfFirst { it.selected }.coerceAtLeast(0) }
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = initialSelection)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val cardWidth = dimensionResource(R.dimen.theme_card_width)
    val labelWidth = with(density) { (cardWidth - spacing.sm * 2).roundToPx() }
    val names = state.cards.map { stringResource(it.theme.nameRes()) }
    val statuses = state.cards.map { themeStatus(it) }
    val nameStyle = MaterialTheme.typography.labelLarge
    val statusStyle = MaterialTheme.typography.labelMedium
    val labelHeight = with(density) {
        names.maxOf { textMeasurer.measure(it, style = nameStyle,
            constraints = Constraints(maxWidth = labelWidth)).size.height }.toDp()
    }.coerceAtLeast(dimensionResource(R.dimen.theme_card_label_height))
    val statusHeight = with(density) {
        statuses.maxOf { textMeasurer.measure(it, style = statusStyle,
            constraints = Constraints(maxWidth = labelWidth)).size.height }.toDp()
    }
    Column {
        if (state.cards.any { !it.unlocked }) Text(stringResource(R.string.theme_unlock_hint),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs))
        LazyRow(
            state = scroll,
            modifier = Modifier.selectableGroup(),
            contentPadding = PaddingValues(dimensionResource(R.dimen.theme_card_shadow_padding)),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(state.cards, key = { it.theme.id() }) { card ->
                ThemeCard(card, labelHeight, statusHeight, !state.busy, onThemeTapped)
            }
        }
        if (state.busy) Text(stringResource(R.string.theme_ad_loading),
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(spacing.sm))
    }
    if (state.lockedThemeNameRes != null) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(stringResource(R.string.theme_locked_title)) },
            text = {
                Text(stringResource(R.string.theme_reward_confirmation,
                    stringResource(state.lockedThemeNameRes)))
            },
            confirmButton = { TextButton(onClick = onWatchAd) { Text(stringResource(R.string.theme_watch_ad)) } },
            dismissButton = { TextButton(onClick = onDismissDialog) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}

@Composable
private fun themeStatus(card: ThemeCardState): String = when {
    card.alwaysFree -> stringResource(R.string.theme_always_free)
    card.pro -> stringResource(R.string.theme_included_pro)
    !card.unlocked -> stringResource(R.string.theme_watch_seven_days)
    else -> {
        val minutes = (card.remainingMillis / 60_000).coerceAtLeast(1)
        val (resource, count) = when {
            minutes >= 24 * 60 -> R.plurals.theme_remaining_days to (minutes / (24 * 60))
            minutes >= 60 -> R.plurals.theme_remaining_hours to (minutes / 60)
            else -> R.plurals.theme_remaining_minutes to minutes
        }
        pluralStringResource(resource, count.toInt(), count.toInt())
    }
}

@Composable
private fun ThemeCard(
    card: ThemeCardState, labelHeight: Dp, statusHeight: Dp, enabled: Boolean,
    onThemeTapped: (String) -> Unit,
) {
    val theme = card.theme
    val spacing = GraphyThemeTokens.spacing
    val name = stringResource(theme.nameRes())
    val status = themeStatus(card)
    val iconSize = dimensionResource(R.dimen.theme_state_icon_size)
    val borderWidth = dimensionResource(if (card.selected) R.dimen.theme_selected_outline else R.dimen.theme_outline)
    Surface(
        modifier = Modifier.width(dimensionResource(R.dimen.theme_card_width))
            .selectable(selected = card.selected, enabled = enabled, role = Role.RadioButton,
                onClick = { onThemeTapped(theme.id()) })
            .semantics { contentDescription = name; stateDescription = status },
        color = Color(theme.background()),
        contentColor = Color(theme.primaryContentColor()),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(borderWidth, Color(if (card.selected) theme.accentContentColor() else theme.secondaryContentColor())),
        shadowElevation = dimensionResource(R.dimen.theme_card_elevation),
    ) {
        Column(
            modifier = Modifier.padding(spacing.sm).clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(Modifier.fillMaxWidth().height(iconSize)) {
                if (!card.unlocked) Icon(painterResource(R.drawable.ic_theme_lock), null,
                    Modifier.size(iconSize).align(Alignment.TopEnd))
                if (card.selected) Icon(painterResource(R.drawable.ic_theme_check), null,
                    Modifier.size(iconSize).align(Alignment.TopStart))
            }
            Surface(
                color = Color(theme.background()), shape = MaterialTheme.shapes.small,
                shadowElevation = dimensionResource(R.dimen.theme_card_elevation),
                modifier = Modifier.fillMaxWidth().height(dimensionResource(R.dimen.theme_swatch_height)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    ThemeColorDot(Color(theme.primaryContentColor()))
                    ThemeColorDot(Color(theme.secondaryContentColor()))
                    ThemeColorDot(Color(theme.accentContentColor()))
                }
            }
            Box(Modifier.size(iconSize + spacing.sm * 2).then(
                if (!card.unlocked) Modifier.border(dimensionResource(R.dimen.theme_outline),
                    Color(theme.primaryContentColor()), CircleShape) else Modifier),
                contentAlignment = Alignment.Center) {
                Icon(painterResource(if (card.unlocked) R.drawable.ic_theme_trophy else R.drawable.ic_play_button_ad),
                    null, Modifier.size(iconSize))
            }
            Box(Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.Center) {
                Text(name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().height(statusHeight), contentAlignment = Alignment.Center) {
                Text(status, color = Color(theme.secondaryContentColor()),
                    style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ThemeColorDot(color: Color) {
    Box(Modifier.padding(horizontal = GraphyThemeTokens.spacing.xs / 2)
        .size(dimensionResource(R.dimen.theme_swatch_color_size)).background(color, CircleShape))
}
