# SETTINGS
- UI: SettingsActivity -> SettingsFragment -> SettingsUi (ComposeView).
- Settings uses GraphyTheme's Material 3 color scheme, typography and spacing. The AppCompat action bar is hidden; SettingsUi owns the TopAppBar. WindowChrome keeps status/navigation bar colors in sync.
- Borderless sections: Appearance, Display, Interaction, Feedback, Information. SettingsRow and SettingsSwitchRow share row spacing/touch targets. SettingsSelectionDialog handles the three simple display choices.
- ThemeSelector and ThemeSelectorViewModel retain the compact theme cards, access states and opt-in rewarded-theme dialog. ThemeManager selection updates GraphyTheme and WindowChrome immediately without Activity recreation.
- Preferences helper retains all existing keys and defaults. The screen listens for SharedPreferences changes so values and switches update even when storage changes elsewhere.
- Actions: Rate (URL), Share (Intent), Website (URL), Privacy (URL).
- Back/Up finish SettingsActivity. The screen is vertically scrollable and its selection dialogs dismiss on Back, outside tap or Cancel.

- Theme access, migration and reward persistence: see [THEME.md](THEME.md#theme-preferences-and-access). The three permanent free themes never expire. Required ad privacy choices are available in Preferences as well as the main overflow menu.
