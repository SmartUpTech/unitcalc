# SETTINGS
- UI: SettingsActivity -> SettingsFragment. Layout: fragment_settings.
- Custom Rows: Row (tv_title, tv_summary, tv_value). Types: Theme (ComposeView/ThemeSelector), Choice (Dialog), Action (Runnable).
- Logic: Preferences helper integration. ChoiceDialog (values from arrays).
- Prefs: PREFS_NUMBER_OF_DECIMALS, PREFS_GROUP_SEPARATOR, PREFS_DECIMAL_SEPARATOR, PREFS_SELECTED_THEME.
- Actions: Rate (URL), Share (Intent), Website (URL), Privacy (URL).
- Theming: applySettingsTheme() -> Card background/stroke (functions color) + ThemeApplier.
- Entry: MainActivity drawer/explore -> SettingsActivity.
- ThemeSelectorController hosts the Compose row through the existing Graphy theme bridge. ThemeSelectorViewModel derives cards/progress from Preferences; locked taps show a Material 3 dialog and available taps use ThemeManager. Selection updates the Settings surface/chrome immediately without Activity recreation. See THEME.md for progression, migration and catalog-rank rules.
