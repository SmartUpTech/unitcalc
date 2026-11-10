# SETTINGS
- UI: SettingsActivity -> SettingsFragment -> SettingsUi (ComposeView).
- Settings uses GraphyTheme's Material 3 color scheme, typography and spacing. The AppCompat action bar is hidden; SettingsUi owns the TopAppBar. WindowChrome keeps status/navigation bar colors in sync.
- Borderless sections in order: General (Start-up screen, Keypad sounds, Keypad vibration), Appearance (existing Theme selector, Buttons, Text Font, Reset customization), Language & region (App language, Region, Currency, Number format, Reset to region defaults), Feedback (Rate on Google Play, Share app), Privacy (Usage analytics, conditional Ad privacy choices, Privacy policy), About (Developer website, App version). SettingsRow and SettingsSwitchRow share row spacing/touch targets. SettingsSelectionDialog keeps raw option values separate from display labels so separator examples never change persisted values or calculation behavior.
- ThemeSelector and ThemeSelectorViewModel retain the compact theme cards, access states and opt-in rewarded-theme dialog. ThemeManager selection updates GraphyTheme and WindowChrome immediately without Activity recreation.
- Preferences helper retains all existing keys and defaults. The screen listens for SharedPreferences changes so values and switches update even when storage changes elsewhere.
- General includes every main calculator, Unit Converter, Currency Converter and Timer in Start-up screen. `CalculatorCatalog.getStartupEntries()` supplies the same ordered choices to settings and preference validation; category-specific unit choices stay inside Unit Converter. Calculator remains the default. `Preferences` stores stable catalog IDs in `startup_screen`. Fresh MainActivity launches apply the chosen screen/mode; saved-instance restoration and explicit Timer launches retain priority. Returning from Preferences does not redirect the current workspace.
- Actions: Rate (URL), Share (Intent), Website (URL), Privacy (URL).
- Privacy includes Usage analytics with an Allowed / Not allowed summary instead of an analytics switch. The themed dialog explains Firebase usage analytics and offers Allow / Do not allow with the current choice selected. Dismissal preserves consent; explicit choices use the existing `UsageAnalytics.setEnabled` path. Existing choices persist, new users default to off, and declining stops collection and resets local analytics data. Ad privacy choices remain separate.
- Back/Up finish SettingsActivity. The screen is vertically scrollable and its selection dialogs dismiss on Back, outside tap or Cancel.

- Theme access, migration and reward persistence: see [THEME.md](THEME.md#theme-preferences-and-access). The three permanent free themes never expire. Required ad privacy choices are available in Preferences as well as the main overflow menu.

- Appearance drafts, shared renderers, defaults and review captures: see [APPEARANCE.md](APPEARANCE.md). Reset affects only button style/remembered shape/font; Theme and Number format retain their existing behavior.

- Text Font now mirrors Buttons: one live keypad row, an “A fresh look with <font name>”
  sample and a horizontal selector with each font name in its own font. All
  preview changes stay local until Apply; cancellation and dismissal preserve the
  applied font. Both dialogs share the selector and native preview component.

Settings group headings explicitly use bold in the selected app font across every theme.

## Language and regional preferences

`RegionalSettingsUi` reuses the shared Settings rows/dialogs and converter picker.
Language, region and preferred currency are independent. AppCompat owns the app
language; Settings text, dialog copy, theme/status labels, country names and
currency names come from native XML resources. Font-family names, currency ISO
codes, unit symbols and language self-names retain their identities.

Settings copy uses everyday app vocabulary. Prefer familiar loanwords where they
are easier to recognize (Hindi वाइब्रेशन, कॉमा, प्राइवेसी, फ़ीडबैक), and retain
natural local terms elsewhere (Turkish titreşim, Indonesian getaran). Explain
technical concepts plainly; preserve consent meaning, numeric examples and
format placeholders. This wording review does not replace native-speaker review.

Fresh installs use en-US, US, USD, Western digits and four displayed decimal
places. The versioned migration preserves existing effective preferences, including
legacy absent-key defaults. Region is persisted explicitly, never inferred from
the app language. A different region resets number overrides and preferred
currency; selecting the same region preserves overrides. Reset keeps language
and saved amounts. New financial scenarios use the preferred currency; saved
scenarios and converter currency choices retain their existing identities.

Number formatting is a staged draft with a live example, decimal/group separators,
Indian/international grouping and 0–10 displayed places. Apply commits the draft;
Cancel, Back and outside dismissal discard it. Separator collisions prevent Apply.
The Settings ComposeView has a stable resource ID for saved draft/dialog state.

Changing language can create a persisted region suggestion. The short prompt is
shown in the new language, after locale recreation settles. Back/outside dismissal
acts as Keep current. Declined language/region pairs are suppressed, and pending
prompts resume in Settings after recreation without interrupting ordinary app
launches. See [LOCALIZATION_IMPLEMENTATION_PLAN.md](LOCALIZATION_IMPLEMENTATION_PLAN.md)
for the full transition contract and [LOCALIZATION_DATA.md](LOCALIZATION_DATA.md)
for territory/currency provenance.
