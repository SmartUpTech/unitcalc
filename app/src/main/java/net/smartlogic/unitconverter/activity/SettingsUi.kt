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
                SettingsScreen(preferences, themes, onBack) {
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
    onThemeChanged: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val spacing = GraphyThemeTokens.spacing
    val themeState by themes.uiState.collectAsStateWithLifecycle()
    val rateUrl = stringResource(R.string.url_app_link)
    val shareBody = stringResource(R.string.note_share_body) +
        stringResource(R.string.url_app_short_link)
    val brandName = stringResource(R.string.brand_name)
    val shareTitle = stringResource(R.string.pref_share_title)
    val websiteUrl = stringResource(R.string.url_smartup_website)
    val privacyUrl = stringResource(R.string.url_privacy_policy)
    var revision by remember { mutableIntStateOf(0) }
    var activeChoiceKey by rememberSaveable { mutableStateOf<String?>(null) }
    val choices = remember {
        listOf(
            Choice(R.string.prefs_title_number_decimals, Preferences.PREFS_NUMBER_OF_DECIMALS,
                R.array.number_decimals, R.string.default_number_decimals),
            Choice(R.string.prefs_title_group_separator, Preferences.PREFS_GROUP_SEPARATOR,
                R.array.group_separators, R.string.default_group_separator),
            Choice(R.string.prefs_title_decimal_separator, Preferences.PREFS_DECIMAL_SEPARATOR,
                R.array.decimal_separators, R.string.default_decimal_separator),
        )
    }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (choices.any { it.key == key } ||
                key == Preferences.PREFS_KEY_SOUNDS || key == Preferences.PREFS_KEY_VIBRATION) {
                revision++
            }
        }
        preferences.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    revision

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
            SettingsSection(R.string.settings_section_appearance)
            Text(stringResource(R.string.prefs_title_theme),
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                style = MaterialTheme.typography.bodyLarge)
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(start = spacing.lg)) {
                ThemeSelector(themeState, onThemeTapped = { id ->
                    if (themes.requestTheme(id) && id != ThemeManager.get().id()) {
                        ThemeManager.select(context, id)
                        onThemeChanged()
                    }
                }, onDismissDialog = themes::dismissLockedTheme)
            }

            SettingsSection(R.string.prefs_title_display)
            choices.forEachIndexed { index, choice ->
                val value = preferences.preferences.getString(choice.key, stringResource(choice.defaultValue))
                    ?: stringResource(choice.defaultValue)
                SettingsRow(
                    title = stringResource(choice.title),
                    value = if (choice.key == Preferences.PREFS_GROUP_SEPARATOR &&
                        (value == "None" || value.isEmpty())) stringResource(R.string.group_separator_none)
                        else value,
                    onClick = { activeChoiceKey = choice.key },
                )
                if (index < choices.lastIndex) SettingsDivider()
            }

            SettingsSection(R.string.settings_section_interaction)
            SettingsSwitchRow(stringResource(R.string.prefs_key_vibration_title),
                preferences.isKeyVibrationEnabled(), preferences::setKeyVibrationEnabled)
            SettingsDivider()
            SettingsSwitchRow(stringResource(R.string.prefs_key_sounds_title),
                preferences.isKeySoundsEnabled(), preferences::setKeySoundsEnabled)

            SettingsSection(R.string.title_activity_feedback)
            SettingsRow(stringResource(R.string.pref_rate_title), onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rateUrl)))
            })
            SettingsDivider()
            SettingsRow(shareTitle, onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, brandName)
                    putExtra(Intent.EXTRA_TEXT, shareBody)
                }
                context.startActivity(Intent.createChooser(intent, shareTitle))
            })

            SettingsSection(R.string.title_activity_info)
            SettingsRow(stringResource(R.string.pref_website_title), onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl)))
            })
            SettingsDivider()
            SettingsRow(stringResource(R.string.pref_privacy_title), onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
            })
            SettingsDivider()
            SettingsRow(stringResource(R.string.pref_version_title), value = BuildConfig.VERSION_NAME)
        }
    }
    choices.firstOrNull { it.key == activeChoiceKey }?.let { choice ->
        val values = stringArrayResource(choice.entries)
        val selected = preferences.preferences.getString(choice.key, stringResource(choice.defaultValue))
        SettingsSelectionDialog(
            title = stringResource(choice.title), values = values, selected = selected,
            onSelect = { value ->
                preferences.preferences.edit().putString(choice.key, value).apply()
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
private fun SettingsRow(title: String, value: String? = null, onClick: (() -> Unit)? = null) {
    val spacing = GraphyThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = dimensionResource(R.dimen.converter_touch_target))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
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
) {
    val spacing = GraphyThemeTokens.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = dimensionResource(R.dimen.settings_dialog_max_height))
                .verticalScroll(rememberScrollState()).selectableGroup()) {
                values.forEach { value ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = dimensionResource(R.dimen.converter_touch_target))
                            .selectable(selected = value == selected, role = Role.RadioButton,
                                onClick = { onSelect(value) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(value, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
