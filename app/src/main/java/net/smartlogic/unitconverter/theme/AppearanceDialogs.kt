package net.smartlogic.unitconverter.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens

/** Only Apply writes preferences. Back, Cancel and outside taps discard the saved draft. */
@Composable
fun ButtonsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var styleName by rememberSaveable { mutableStateOf(Appearance.style(context).name) }
    var shapeName by rememberSaveable { mutableStateOf(Appearance.shape(context).name) }
    val style = Appearance.Style.valueOf(styleName)
    val shape = Appearance.Shape.valueOf(shapeName)
    val spacing = GraphyThemeTokens.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance_buttons)) },
        text = {
            Column(Modifier.heightIn(max = dimensionResource(R.dimen.settings_dialog_max_height))) {
                Text(stringResource(R.string.appearance_preview), style = MaterialTheme.typography.bodyMedium)
                KeypadPreview(style, shape)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.appearance_style), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = spacing.sm))
                    AppearanceChoiceStrip(
                        Appearance.Style.entries.map { it.name to it.label }, styleName,
                        onSelect = { styleName = it },
                    )
                    Text(stringResource(R.string.appearance_shape), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = spacing.sm))
                    val enabled = style != Appearance.Style.FLAT
                    if (!enabled) Text(stringResource(R.string.appearance_flat_shape),
                        style = MaterialTheme.typography.bodyMedium)
                    AppearanceChoiceStrip(
                        Appearance.Shape.entries.map { it.name to it.label }, shapeName, enabled,
                        onSelect = { shapeName = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Appearance.applyButtons(context, style, shape)
                onDismiss()
            }) { Text(stringResource(R.string.appearance_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

/** A shared, single-choice horizontal strip; the only keypad preview lives above both strips. */
@Composable
private fun AppearanceChoiceStrip(options: List<Pair<String, Int>>, selected: String,
    enabled: Boolean = true, onSelect: (String) -> Unit) {
    val spacing = GraphyThemeTokens.spacing
    val scroll = rememberScrollState()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(dimensionResource(R.dimen.appearance_key_radius))
    val fill = colors.onSurface.copy(alpha = 0.06f).compositeOver(colors.surface)
    val outline = dimensionResource(R.dimen.theme_selected_outline)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().horizontalScroll(scroll).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            options.forEach { (id, label) ->
                val isSelected = selected == id
                val bringIntoView = remember { BringIntoViewRequester() }
                LaunchedEffect(isSelected) {
                    if (isSelected) {
                        withFrameNanos { }
                        bringIntoView.bringIntoView()
                    }
                }
                Row(Modifier.bringIntoViewRequester(bringIntoView)
                    .heightIn(min = dimensionResource(R.dimen.converter_touch_target))
                    .clip(shape).background(fill)
                    .border(outline, if (isSelected) colors.primary else Color.Transparent, shape)
                    .selectable(isSelected, enabled = enabled, role = Role.RadioButton,
                        onClick = { onSelect(id) })
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) Icon(painterResource(R.drawable.ic_theme_check), null,
                        Modifier.size(dimensionResource(R.dimen.converter_chevron_size)),
                        tint = if (enabled) colors.primary else colors.onSurfaceVariant)
                    Text(stringResource(label), maxLines = 1, style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) colors.onSurface else colors.onSurfaceVariant)
                }
            }
        }
        Canvas(Modifier.padding(top = spacing.sm)
            .width(dimensionResource(R.dimen.appearance_scroll_track_width))
            .height(dimensionResource(R.dimen.appearance_scroll_track_height))) {
            if (scroll.maxValue > 0) {
                val corner = CornerRadius(size.height / 2)
                drawRoundRect(colors.onSurface.copy(alpha = 0.08f), cornerRadius = corner)
                val fraction = scroll.viewportSize.toFloat() / (scroll.viewportSize + scroll.maxValue)
                val thumb = size.width * fraction.coerceIn(0.15f, 1f)
                val progress = scroll.value.toFloat() / scroll.maxValue
                val x = (size.width - thumb) * if (rtl) 1f - progress else progress
                drawRoundRect(colors.onSurface.copy(alpha = if (enabled) 0.5f else 0.2f),
                    topLeft = Offset(x, 0f), size = Size(thumb, size.height), cornerRadius = corner)
            }
        }
    }
}

/** Native renderer shared with actual keypads: includes the accent-filled Equal exception. */
@Composable
internal fun KeypadPreview(style: Appearance.Style, shape: Appearance.Shape) {
    val theme = net.smartlogic.unitconverter.graphy.compose.theme.LocalCalculatorTheme.current
    val font = Appearance.font(LocalContext.current)
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gap = dimensionResource(R.dimen.appearance_key_gap)
    val paint = android.graphics.Paint().apply {
        typeface = ResourcesCompat.getFont(context, font.resource)
        textSize = context.resources.getDimension(R.dimen.appearance_preview_text)
    }
    val textHeight = with(density) { (paint.fontMetrics.descent - paint.fontMetrics.ascent).toDp() }
    val size = maxOf(dimensionResource(R.dimen.appearance_preview_height), textHeight + gap * 2)
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(size),
        factory = { context -> KeypadPreviewView(context) },
        update = { it.render(theme, style, shape, font) },
    )
}

@Composable
fun TextFontDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var draft by rememberSaveable { mutableStateOf(Appearance.font(context).name) }
    val spacing = GraphyThemeTokens.spacing
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance_font)) },
        text = {
            Column(Modifier.heightIn(max = dimensionResource(R.dimen.settings_dialog_max_height))
                .verticalScroll(rememberScrollState()).selectableGroup()) {
                Appearance.Font.entries.forEach { font ->
                    Column(Modifier.fillMaxWidth().selectable(draft == font.name,
                        role = Role.RadioButton, onClick = { draft = font.name })
                        .padding(bottom = spacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(draft == font.name, onClick = null)
                            Text(font.label, fontFamily = FontFamily(Font(font.resource)))
                        }
                        Text(stringResource(R.string.appearance_font_sample),
                            fontFamily = FontFamily(Font(font.resource)), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            Appearance.applyFont(context, Appearance.Font.valueOf(draft)); onDismiss()
        }) { Text(stringResource(R.string.appearance_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } })
}
