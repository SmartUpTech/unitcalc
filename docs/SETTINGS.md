# SETTINGS
- UI: SettingsActivity -> SettingsFragment -> SettingsUi (ComposeView).
- Settings uses GraphyTheme's Material 3 color scheme, typography and spacing. The AppCompat action bar is hidden; SettingsUi owns the TopAppBar. WindowChrome keeps status/navigation bar colors in sync.
- Borderless sections: Appearance, Display, Interaction, Feedback, Information. SettingsRow and SettingsSwitchRow share row spacing/touch targets. SettingsSelectionDialog handles the three simple display choices.
- ThemeSelector and ThemeSelectorViewModel retain the compact theme cards, access states and opt-in rewarded-theme dialog. ThemeManager selection updates GraphyTheme and WindowChrome immediately without Activity recreation.
- Preferences helper retains all existing keys and defaults. The screen listens for SharedPreferences changes so values and switches update even when storage changes elsewhere.
- Interaction includes Start-up screen: Calculator (default), Unit Converter or Currency Converter. `Preferences` stores stable catalog IDs in `startup_screen`. Fresh MainActivity launches apply the chosen screen/mode; saved-instance restoration and explicit Timer launches retain priority. Returning from Preferences does not redirect the current workspace.
- Actions: Rate (URL), Share (Intent), Website (URL), Privacy (URL).
- Information includes Privacy choices instead of an analytics switch. The themed dialog explains Firebase usage analytics and offers Allow / Do not allow with the current choice selected. Dismissal preserves consent; explicit choices use the existing `UsageAnalytics.setEnabled` path. Existing choices persist, new users default to off, and declining stops collection and resets local analytics data. Ad privacy choices remain separate.
- Back/Up finish SettingsActivity. The screen is vertically scrollable and its selection dialogs dismiss on Back, outside tap or Cancel.

- Theme access, migration and reward persistence: see [THEME.md](THEME.md#theme-preferences-and-access). The three permanent free themes never expire. Required ad privacy choices are available in Preferences as well as the main overflow menu.
