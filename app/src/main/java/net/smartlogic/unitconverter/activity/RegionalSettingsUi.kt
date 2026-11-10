package net.smartlogic.unitconverter.activity

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.converter.ConverterPickerDialogFragment
import net.smartlogic.unitconverter.helper.*
import java.math.BigDecimal

internal const val REGION_PICKER = 100

@Composable
internal fun RegionalSettings(preferences: Preferences, activity: FragmentActivity) {
    var revision by remember { mutableIntStateOf(0) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val suggestions = remember(preferences) { RegionSuggestion(preferences.preferences) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    revision
    val profile = preferences.regionalProfile
    val language = AppLanguage.current(activity)
    SettingsSection(R.string.regional_section)
    SettingsRow(stringResource(R.string.regional_language), summary = stringResource(language.nativeNameResource),
        onClick = { dialog = "language" })
    SettingsRow(stringResource(R.string.regional_region), summary = stringResource(profile.region.nameResource),
        onClick = { showRegions(activity, profile.region.code) })
    val currency = PreferredCurrency.find(activity, profile.currency)
    SettingsRow(stringResource(R.string.regional_currency),
        summary = stringResource(R.string.regional_currency_label, profile.currency, currency.currencyName),
        onClick = { PreferredCurrency.showPicker(activity) })
    SettingsRow(stringResource(R.string.regional_numbers),
        summary = stringResource(R.string.regional_format_summary,
            profile.numbers.format(BigDecimal("1234567.89")),
            stringResource(if (profile.customNumbers) R.string.regional_custom else R.string.regional_default)),
        onClick = { dialog = "numbers" })
    SettingsRow(stringResource(R.string.regional_reset), summary = stringResource(R.string.regional_reset_description),
        onClick = { preferences.resetRegionDefaults() })
    if (dialog == "language") SettingsSelectionDialog(
        title = stringResource(R.string.regional_language),
        values = SupportedLanguage.ALL.map { it.tag }.toTypedArray(),
        valueLabels = SupportedLanguage.ALL.map { stringResource(it.nativeNameResource) }.toTypedArray(),
        selected = language.tag,
        onSelect = { dialog = null; AppLanguage.select(activity, SupportedLanguage.resolve(it)) },
        onDismiss = { dialog = null })
    if (dialog == "numbers") RegionalNumberDialog(profile.numbers,
        onApply = { preferences.setNumberFormat(it); dialog = null }, onDismiss = { dialog = null })
    val pending = suggestions.pending(profile.region)
    val renderedLanguage = SupportedLanguage.resolve(
        androidx.compose.ui.platform.LocalConfiguration.current.locales[0].toLanguageTag())
    if (pending != null && pending.tag == renderedLanguage.tag) AlertDialog(
        onDismissRequest = { suggestions.keep(profile.region) },
        title = { Text(stringResource(R.string.regional_suggest_title,
            stringResource(RegionRegistry.find(pending.suggestedRegion).nameResource))) },
        text = { Text(stringResource(R.string.regional_reset_description)) },
        confirmButton = { TextButton(onClick = { preferences.applyRegion(pending.suggestedRegion) }) {
            Text(stringResource(R.string.regional_switch)) } },
        dismissButton = { TextButton(onClick = { suggestions.keep(profile.region) }) {
            Text(stringResource(R.string.regional_keep)) } })
}

private fun showRegions(activity: FragmentActivity, selected: String) {
    val manager = activity.supportFragmentManager
    if (manager.isStateSaved || manager.findFragmentByTag(ConverterPickerDialogFragment.TAG) != null) return
    val regions = RegionRegistry.ALL
    val labels = ArrayList(regions.map { activity.getString(it.nameResource) })
    val details = ArrayList(regions.map { it.code })
    val keywords = ArrayList(regions.map { activity.getString(it.nameResource) + " " + it.englishName + " " + it.code })
    ConverterPickerDialogFragment.newInstance(
        activity.getString(R.string.regional_region), false, REGION_PICKER,
        regions.indexOfFirst { it.code == selected }, labels, details, keywords, null
    ).useLabelResources(R.string.regional_region, regions.map { it.nameResource }.toIntArray()).apply {
        requireArguments().putBoolean("regional_note", true)
        requireArguments().putInt("search_hint", R.string.regional_search)
    }
        .showNow(manager, ConverterPickerDialogFragment.TAG)
}

@Composable
private fun RegionalNumberDialog(initial: NumberFormatSettings,
    onApply: (NumberFormatSettings) -> Unit, onDismiss: () -> Unit) {
    var decimal by rememberSaveable { mutableStateOf(initial.decimalSeparator.toString()) }
    var group by rememberSaveable { mutableStateOf(initial.groupSeparator) }
    var pattern by rememberSaveable { mutableStateOf(initial.groupingPattern) }
    var places by rememberSaveable { mutableStateOf(initial.decimalPlaces.toString()) }
    var choice by rememberSaveable { mutableStateOf<String?>(null) }
    val valid = decimal != group
    val draft = if (valid) NumberFormatSettings(decimal[0], group, pattern, places.toInt()) else null
    val separators = listOf("", ",", ".", " ", "\u00a0", "\u202f", "'")
    val separatorLabels = listOf(R.string.group_separator_none, R.string.regional_comma,
        R.string.regional_dot, R.string.regional_space, R.string.regional_nbsp,
        R.string.regional_nnbsp, R.string.regional_apostrophe)
    fun separatorResource(value: String) = separators.indexOf(value).let {
        if (it < 0) R.string.regional_default else separatorLabels[it]
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.regional_numbers)) },
        text = { Column(Modifier.heightIn(max = dimensionResource(R.dimen.settings_dialog_max_height))
            .verticalScroll(rememberScrollState())) {
            if (draft != null) Text(draft.format(BigDecimal("1234567.89")))
            else Text(stringResource(R.string.regional_collision), color = MaterialTheme.colorScheme.error)
            SettingsRow(stringResource(R.string.prefs_title_decimal_separator),
                summary = stringResource(separatorResource(decimal)), onClick = { choice = "decimal" })
            SettingsRow(stringResource(R.string.prefs_title_group_separator),
                summary = stringResource(separatorResource(group)), onClick = { choice = "group" })
            SettingsRow(stringResource(R.string.regional_pattern),
                summary = stringResource(if (pattern == "indian") R.string.regional_indian else R.string.regional_international),
                onClick = { choice = "pattern" })
            SettingsRow(stringResource(R.string.prefs_title_number_decimals), value = places,
                onClick = { choice = "places" })
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { draft?.let(onApply) }) {
            Text(stringResource(R.string.regional_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } })
    if (choice != null) {
        val values = when (choice) {
            "decimal" -> listOf(".", ",")
            "group" -> separators
            "pattern" -> listOf("international", "indian")
            else -> (0..10).map { it.toString() }
        }
        val labels = when (choice) {
            "decimal", "group" -> values.map { stringResource(separatorResource(it)) }
            "pattern" -> listOf(stringResource(R.string.regional_international), stringResource(R.string.regional_indian))
            else -> values
        }
        val title = when (choice) {
            "decimal" -> R.string.prefs_title_decimal_separator
            "group" -> R.string.prefs_title_group_separator
            "pattern" -> R.string.regional_pattern
            else -> R.string.prefs_title_number_decimals
        }
        SettingsSelectionDialog(stringResource(title), values.toTypedArray(),
            when (choice) { "decimal" -> decimal; "group" -> group; "pattern" -> pattern; else -> places },
            onSelect = { when (choice) { "decimal" -> decimal = it; "group" -> group = it
                "pattern" -> pattern = it; else -> places = it }; choice = null },
            onDismiss = { choice = null }, valueLabels = labels.toTypedArray())
    }
}
