package net.smartlogic.unitconverter.activity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.smartlogic.unitconverter.BuildConfig
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.helper.Preferences
import net.smartlogic.unitconverter.model.CalculatorCatalog
import net.smartlogic.unitconverter.theme.Appearance
import net.smartlogic.unitconverter.theme.ButtonsDialog
import net.smartlogic.unitconverter.theme.TextFontDialog
import net.smartlogic.unitconverter.theme.ThemeManager
import net.smartlogic.unitconverter.theme.ThemeSelector
import net.smartlogic.unitconverter.theme.ThemeSelectorViewModel
import net.smartlogic.unitconverter.theme.WindowChrome

/** The existing Fragment owns the theme ViewModel and the ComposeView lifecycle. */
object SettingsUi {
    @JvmStatic
    fun bind(view: ComposeView, fragment: Fragment, onBack: Runnable) {
        val preferences = Preferences.getInstance(fragment.requireContext())
        val themes = ViewModelProvider(
            fragment, ThemeSelectorViewModel.Factory(preferences)
        ).get(ThemeSelectorViewModel::class.java)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            GraphyTheme {
                SettingsScreen(preferences, themes, onBack, fragment.requireActivity()) {
                    WindowChrome.apply(fragment.requireActivity())
                }
            }
        }
    }
}

private data class Choice(val title: Int, val key: String, val entries: Int, val defaultValue: Int)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    preferences: Preferences,
    themes: ThemeSelectorViewModel,
    onBack: Runnable,
    activity: android.app.Activity,
    onThemeChanged: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val spacing = GraphyThemeTokens.spacing
    val themeState by themes.uiState.collectAsStateWithLifecycle()
    val rateUrl = stringResource(R.string.url_app_link)
    val shareBody = stringResource(R.string.note_share_body, stringResource(R.string.brand_name), rateUrl)
    val shareSubject = stringResource(R.string.share_app_subject, stringResource(R.string.brand_name))
    val shareTitle = stringResource(R.string.pref_share_title)
    val websiteUrl = stringResource(R.string.url_smartup_website)
    val privacyUrl = stringResource(R.string.url_privacy_policy)
    var revision by remember { mutableIntStateOf(0) }
    var activeChoiceKey by rememberSaveable { mutableStateOf<String?>(null) }
    val choices = remember {
        listOf(
            Choice(R.string.prefs_title_number_decimals, Preferences.PREFS_NUMBER_OF_DECIMALS,
                R.array.number_decimals, R.string.default_number_decimals),
            Choice(R.string.prefs_title_decimal_separator, Preferences.PREFS_DECIMAL_SEPARATOR,
                R.array.decimal_separators, R.string.default_decimal_separator),
            Choice(R.string.prefs_title_group_separator, Preferences.PREFS_GROUP_SEPARATOR,
                R.array.group_separators, R.string.default_group_separator),
        )
    }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (Appearance.isPreference(key) || key == net.smartlogic.unitconverter.helper.UsageAnalytics.ENABLED ||
                key == Preferences.PREFS_STARTUP_SCREEN || choices.any { it.key == key } ||
                key == Preferences.PREFS_KEY_SOUNDS || key == Preferences.PREFS_KEY_VIBRATION) {
                revision++
            }
        }
        preferences.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    revision
    DisposableEffect(activity) {
        val listener = ThemeManager.Listener { WindowChrome.apply(activity) }
        ThemeManager.addListener(listener)
        onDispose { ThemeManager.removeListener(listener) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_settings), maxLines = 1,
                    overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { onBack.run() }) {
                        Icon(painterResource(R.drawable.ic_settings_back),
                            contentDescription = stringResource(R.string.settings_navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.lg),
        ) {
            SettingsSection(R.string.settings_section_general)
            val startupLabels = arrayOf(stringResource(R.string.calculator),
                stringResource(R.string.drawer_unit_converter), stringResource(R.string.startup_currency_converter))
            val startupIds = listOf(CalculatorCatalog.ID_BASIC, CalculatorCatalog.ID_UNIT, CalculatorCatalog.ID_CURRENCY)
            SettingsRow(stringResource(R.string.prefs_startup_screen),
                value = startupLabels[startupIds.indexOf(preferences.startupScreen)],
                onClick = { activeChoiceKey = Preferences.PREFS_STARTUP_SCREEN })
            SettingsDivider()
            SettingsSwitchRow(stringResource(R.string.prefs_key_sounds_title),
                preferences.isKeySoundsEnabled(), preferences::setKeySoundsEnabled)
            SettingsDivider()
            SettingsSwitchRow(stringResource(R.string.prefs_key_vibration_title),
                preferences.isKeyVibrationEnabled(), preferences::setKeyVibrationEnabled)

            SettingsSection(R.string.settings_section_appearance)
            Text(stringResource(R.string.appearance_customise),
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.prefs_title_theme),
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                style = MaterialTheme.typography.bodyLarge)
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(start = spacing.lg)) {
                ThemeSelector(themeState, onThemeTapped = { id ->
                    themes.tapTheme(activity, id)
                    onThemeChanged()
                }, onDismissDialog = themes::dismissLockedTheme,
                    onWatchAd = { themes.watchAd(activity) })
            }

            SettingsRow(stringResource(R.string.appearance_buttons),
                summary = stringResource(Appearance.style(context).label),
                onClick = { activeChoiceKey = Appearance.STYLE })
            SettingsDivider()
            SettingsRow(stringResource(R.string.appearance_font), value = Appearance.font(context).label,
                onClick = { activeChoiceKey = Appearance.FONT })
            SettingsRow(stringResource(R.string.appearance_reset),
                onClick = { activeChoiceKey = "appearance_reset" })

            SettingsSection(R.string.settings_section_number_format)
            choices.forEachIndexed { index, choice ->
                val value = preferences.preferences.getString(choice.key, stringResource(choice.defaultValue))
                    ?: stringResource(choice.defaultValue)
                SettingsRow(
                    title = stringResource(choice.title),
                    value = if (choice.key == Preferences.PREFS_NUMBER_OF_DECIMALS) value else null,
                    summary = if (choice.key != Preferences.PREFS_NUMBER_OF_DECIMALS)
                        separatorLabel(choice.key, value) else null,
                    onClick = { activeChoiceKey = choice.key },
                )
                if (index < choices.lastIndex) SettingsDivider()
            }

            SettingsSection(R.string.settings_section_privacy)
            SettingsRow(stringResource(R.string.settings_privacy_choices),
                summary = stringResource(if (preferences.preferences.getBoolean(
                    net.smartlogic.unitconverter.helper.UsageAnalytics.ENABLED, false))
                    R.string.settings_analytics_allowed else R.string.settings_analytics_not_allowed),
                onClick = { activeChoiceKey = net.smartlogic.unitconverter.helper.UsageAnalytics.ENABLED })
            val ads = net.smartlogic.unitconverter.helper.AdMobManager.getInstance(context)
            if (ads.isGamesPrivacyOptionsRequired) {
                SettingsDivider()
                SettingsRow(stringResource(R.string.games_privacy_options), onClick = {
                    if (!net.smartlogic.unitconverter.theme.ThemeRewardCoordinator.isBusy())
                        ads.showGamesPrivacyOptions(activity)
                })
            }
            SettingsDivider()
            SettingsRow(stringResource(R.string.pref_privacy_title), onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
            })

            SettingsSection(R.string.settings_section_rate_share)
            SettingsRow(stringResource(R.string.pref_rate_title), onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rateUrl)))
            })
            SettingsDivider()
            SettingsRow(shareTitle, onClick = {
                net.smartlogic.unitconverter.helper.UsageAnalytics.event("share_opened", "content_type", "app")
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                    putExtra(Intent.EXTRA_TEXT, shareBody)
                }
                context.startActivity(Intent.createChooser(intent, shareTitle))
            })

            SettingsSection(R.string.settings_section_about)
            SettingsRow(stringResource(R.string.pref_website_title), onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl)))
            })
            SettingsDivider()
            SettingsRow(stringResource(R.string.pref_version_title), value = BuildConfig.VERSION_NAME)
        }
    }
    if (activeChoiceKey == Appearance.STYLE) ButtonsDialog { activeChoiceKey = null }
    if (activeChoiceKey == Appearance.FONT) TextFontDialog { activeChoiceKey = null }
    if (activeChoiceKey == "appearance_reset") {
        AlertDialog(onDismissRequest = { activeChoiceKey = null },
            title = { Text(stringResource(R.string.appearance_reset)) },
            text = { Text(stringResource(R.string.appearance_reset_message)) },
            confirmButton = { TextButton(onClick = { Appearance.reset(context); activeChoiceKey = null }) {
                Text(stringResource(R.string.appearance_reset))
            } },
            dismissButton = { TextButton(onClick = { activeChoiceKey = null }) {
                Text(stringResource(android.R.string.cancel))
            } })
    }
    if (activeChoiceKey == net.smartlogic.unitconverter.helper.UsageAnalytics.ENABLED) {
        val allow = stringResource(R.string.settings_analytics_allow)
        val decline = stringResource(R.string.settings_analytics_decline)
        val enabled = preferences.preferences.getBoolean(
            net.smartlogic.unitconverter.helper.UsageAnalytics.ENABLED, false)
        SettingsSelectionDialog(
            title = stringResource(R.string.settings_privacy_choices),
            description = stringResource(R.string.settings_usage_analytics_summary),
            values = arrayOf(allow, decline), selected = if (enabled) allow else decline,
            onSelect = { value ->
                val granted = value == allow
                net.smartlogic.unitconverter.helper.UsageAnalytics.setEnabled(context, granted)
                if (granted && !enabled) net.smartlogic.unitconverter.helper.UsageAnalytics.event(
                    "theme_active", "theme_id", ThemeManager.get().id())
                activeChoiceKey = null
            },
            onDismiss = { activeChoiceKey = null },
        )
    }
    if (activeChoiceKey == Preferences.PREFS_STARTUP_SCREEN) {
        val ids = listOf(CalculatorCatalog.ID_BASIC, CalculatorCatalog.ID_UNIT, CalculatorCatalog.ID_CURRENCY)
        val labels = arrayOf(stringResource(R.string.calculator),
            stringResource(R.string.drawer_unit_converter), stringResource(R.string.startup_currency_converter))
        SettingsSelectionDialog(
            title = stringResource(R.string.prefs_startup_screen), values = labels,
            selected = labels[ids.indexOf(preferences.startupScreen)],
            onSelect = { label ->
                preferences.setStartupScreen(ids[labels.indexOf(label)])
                activeChoiceKey = null
            },
            onDismiss = { activeChoiceKey = null },
        )
    }
    choices.firstOrNull { it.key == activeChoiceKey }?.let { choice ->
        val values = stringArrayResource(choice.entries)
        val selected = preferences.preferences.getString(choice.key, stringResource(choice.defaultValue))
        SettingsSelectionDialog(
            title = stringResource(choice.title), values = values, selected = selected,
            valueLabels = values.map { separatorLabel(choice.key, it) }.toTypedArray(),
            onSelect = { value ->
                preferences.preferences.edit().putString(choice.key, value).apply()
                net.smartlogic.unitconverter.helper.UsageAnalytics.event("setting_changed", "setting", choice.key)
                activeChoiceKey = null
            },
            onDismiss = { activeChoiceKey = null },
        )
    }
}

@Composable
private fun SettingsSection(title: Int) {
    val spacing = GraphyThemeTokens.spacing
    Text(stringResource(title), modifier = Modifier.fillMaxWidth()
        .padding(start = spacing.lg, end = spacing.lg, top = spacing.lg, bottom = spacing.sm),
        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SettingsRow(title: String, value: String? = null, summary: String? = null,
    onClick: (() -> Unit)? = null) {
    val spacing = GraphyThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = dimensionResource(R.dimen.converter_touch_target))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (value != null) {
            Spacer(Modifier.width(spacing.sm))
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        }
        if (onClick != null) {
            Spacer(Modifier.width(spacing.sm))
            Icon(painterResource(R.drawable.ic_settings_chevron), contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.converter_chevron_size)),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val spacing = GraphyThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = dimensionResource(R.dimen.converter_touch_target))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(spacing.sm))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsDivider() {
    val spacing = GraphyThemeTokens.spacing
    HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.lg),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
}

@Composable
private fun SettingsSelectionDialog(
    title: String, values: Array<String>, selected: String?,
    onSelect: (String) -> Unit, onDismiss: () -> Unit,
    description: String? = null,
    valueLabels: Array<String> = values,
) {
    val spacing = GraphyThemeTokens.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = dimensionResource(R.dimen.settings_dialog_max_height))
                .verticalScroll(rememberScrollState()).selectableGroup()) {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = spacing.sm))
                }
                values.forEachIndexed { index, value ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = dimensionResource(R.dimen.converter_touch_target))
                            .selectable(selected = value == selected, role = Role.RadioButton,
                                onClick = { onSelect(value) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(valueLabels[index], style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

/** Display-only examples; selections and persistence always use the original raw value. */
@Composable
private fun separatorLabel(key: String, value: String): String {
    return when (key) {
        Preferences.PREFS_DECIMAL_SEPARATOR -> when (value) {
            "." -> stringResource(R.string.settings_decimal_point_example)
            "," -> stringResource(R.string.settings_decimal_comma_example)
            else -> value
        }
        Preferences.PREFS_GROUP_SEPARATOR -> when (value) {
            "," -> stringResource(R.string.settings_group_comma_example)
            "." -> stringResource(R.string.settings_group_point_example)
            " " -> stringResource(R.string.settings_group_space_example)
            "", "None", stringResource(R.string.group_separator_none) ->
                stringResource(R.string.settings_group_none_example)
            else -> value
        }
        else -> value
    }
}
