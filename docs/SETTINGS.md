# SETTINGS
- UI: SettingsActivity -> SettingsFragment. Layout: fragment_settings.
- Custom Rows: Row (tv_title, tv_summary, tv_value). Types: Theme (RecyclerView), Choice (Dialog), Action (Runnable).
- Logic: Preferences helper integration. ChoiceDialog (values from arrays).
- Prefs: PREFS_NUMBER_OF_DECIMALS, PREFS_GROUP_SEPARATOR, PREFS_DECIMAL_SEPARATOR, PREFS_SELECTED_THEME.
- Actions: Rate (URL), Share (Intent), Website (URL), Privacy (URL).
- Theming: applySettingsTheme() -> Card background/stroke (functions color) + ThemeApplier.
- Entry: MainActivity drawer/explore -> SettingsActivity.
