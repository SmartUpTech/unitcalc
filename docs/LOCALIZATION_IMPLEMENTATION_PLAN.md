# Localization and regional preferences: implementation plan

Status: approved product decisions consolidated for implementation. No app code is changed by this document.

## 1. Outcome and non-negotiable requirements

Deliver complete, offline, resource-based localization with independent language, region, currency and editable number formatting. Offer a short region suggestion after an incompatible language change. Preserve calculation accuracy, saved data, current navigation and the shared design system.

- Fresh installation: English (United States), United States region, USD, Western digits 0–9, US grouping/separators and four displayed decimal places where the existing tool supports that setting.
- Language changes text only unless the user accepts a separate region suggestion.
- A different region resets all number-format overrides and preferred currency to its defaults. This includes decimal places, reset to the app's four-place default; countries do not define calculator precision.
- Users can override number formatting and currency afterward.
- Selecting the already-active region preserves overrides. The explicit reset action restores defaults for that region.
- Computation precision, rounding, formulas, unit factors, financial assumptions, timers, saved currency identities and rate-provider behavior remain unchanged.
- All app-owned translatable text lives in locale-qualified Android strings.xml resources. No translation libraries, runtime translators, translation APIs or network requirement.
- Numeric output and keypad digits always use Western 0–9. Decimal/grouping conventions can vary. No native-digit preference.
- User-facing messages are short. Translations are friendly, professional, standard and natural, without slang or unnecessary formality.

## 2. Translation options and region availability

Provide these 14 translation options representing 13 languages. A translation's region suffix identifies wording, not the user's selected formatting region.

| Option | Native label | Translation tag | Suggested region when needed |
| --- | --- | --- | --- |
| English (United States) | English (United States) | en-US | US |
| Hindi | हिन्दी | hi-IN | IN |
| Spanish (Spain) | Español (España) | es-ES | ES |
| Spanish (Mexico) | Español (México) | es-MX | MX |
| French | Français | fr-FR | FR |
| Portuguese (Brazil) | Português (Brasil) | pt-BR | BR |
| Russian | Русский | ru-RU | RU |
| Indonesian | Bahasa Indonesia | id-ID | ID |
| German | Deutsch | de-DE | DE |
| Turkish | Türkçe | tr-TR | TR |
| Vietnamese | Tiếng Việt | vi-VN | VN |
| Italian | Italiano | it-IT | IT |
| Polish | Polski | pl-PL | PL |
| Dutch | Nederlands | nl-NL | NL |

Chinese, Korean and Arabic translations are excluded. Countries using those languages can still be selected as regions.

Offer all ISO countries/territories whose current legal-tender currency is supported by the existing preferred-currency catalog. Currency availability, not translation availability, determines region eligibility. Include countries sharing a currency as separate regions, including individual euro-area countries. Do not use localized currency-country descriptions as country identifiers or treat a currency as a country.

Create a checked-in region registry using ISO territory codes. Verify territory/currency associations against current authoritative data before adding them; reuse supported currency identities and do not expand exchange-rate coverage as part of this work. Exclude territories with no supported currency rather than silently assigning USD. Required regions include US, IN, CA, AU, GB and all suggested regions above.

Each region has a deliberate, stable formatting locale/preset, supported default currency and common-language associations. For multilingual countries, use one default formatting preset in v1, with manual formatting overrides available. For example, CA uses en-CA formatting even with French app text. This preserves the rule that changing language alone cannot change number formatting. Date/time ordering follows the region; month/day words and surrounding labels follow the selected language. Preserve existing calendar, time-zone and timer semantics.

## 3. Settings experience

Add the group **Language & region** after Appearance. Move the existing Preferred currency and Number format settings into it. Settings and secondary pickers may scroll; calculator/converter/tool content may not.

Rows, in order:

1. App language — selected native name.
2. Region — localized country/territory name.
3. Currency — code and localized currency name, e.g. USD · US Dollar.
4. Number format — a localized numeric example plus Regional default or Custom.
5. Reset to region defaults — short description: Resets number format and currency.

Use existing Compose Settings rows, shared themed selection dialogs and the searchable native picker where appropriate. Do not change framework, typography, palettes or keypad appearance. Give selections accessible radio/check semantics and minimum 48dp action targets.

### Language selection

- Show native names, with regional qualifiers for the two Spanish choices and Brazilian Portuguese.
- Apply on selecting a language; dismissing the picker changes nothing.
- Do not offer automatic device-language selection in this version. Initial English must be explicit and independent of device language.
- Preserve the selected region and overrides while the new translation is applied.
- After a real language change, evaluate the region-suggestion rules below. Choosing the same translation does nothing.

### Region selection

- Search localized country names, English names and territory codes. Use text labels; flags must not be the only identification.
- Show the brief note: Changing region resets number format and currency.
- Picking a different region applies the region, its currency and its complete formatting defaults as one logical transaction. No additional confirmation is needed in the explicit region picker.
- Keep the current currency-converter source/destination choices and existing financial drafts unchanged.
- Picking the same region does not reset anything.

### Number-format dialog

Edit a local draft, show a live sample and expose Apply and Cancel. Dismissal preserves applied settings.

| Control | Supported choices |
| --- | --- |
| Decimal separator | Dot or comma |
| Group separator | None, comma, dot, ordinary space, non-breaking space, narrow non-breaking space, apostrophe |
| Grouping pattern | International: 1,234,567; Indian: 12,34,567 |
| Decimal places | Existing 0–10 range |

Always show 0–9 digits. Preserve other existing display conventions unless explicitly covered here. Use stable separator/pattern values independently of their translated labels. When grouping is disabled, ignore its pattern during rendering but retain the draft selection. Reject decimal/group separator collisions with a short inline error; never silently change a value to fix another control.

Regional defaults may use a platform-provided separator outside the custom list; preserve and label that regional default correctly until the user explicitly replaces it. Region defaults enable the region's standard grouping. Preserve existing trailing-zero and tool-specific fixed-scale behavior. The decimal preference governs only tools that already permit it; do not change financial formulas or rounding to match currency minor units.

### Currency and reset

Reuse the existing preferred-currency picker and supported list. A currency override only changes defaults for newly created finance scenarios. Returning to the region's currency by manual selection is valid.

Reset to region defaults immediately restores the active region's number format, four-place app precision default and currency in one logical transaction. It keeps language, current numeric values, saved scenarios, history, converter selections, themes and other settings.

## 4. Optional region suggestion after language changes

Keep the suggestion short and predictable:

- Title: Switch region to Germany? (country is a resource placeholder.)
- Message: Resets number format and currency.
- Actions: Switch / Keep current.
- Display the suggestion in the newly selected language.

Define a common-language match using a checked-in territory/language association table derived from a pinned authoritative CLDR snapshot. Include official and de facto official languages, plus explicitly require English to match US, IN, CA and AU. Spanish translation variants both match common Spanish-speaking regions; the ES/MX difference controls the suggested destination, not whether Spanish is valid there. French matches Canada as well as France; Portuguese matches Portugal as well as Brazil. Filter associations to offered regions. Record the source/version and any project overrides.

The transition rules are:

1. If the current region commonly uses the selected language, do not suggest anything.
2. If region already equals the suggested region, do not suggest anything.
3. If the user previously kept this base-language/current-region combination, do not suggest again. Spanish variants share this suppression key.
4. Otherwise apply language immediately, then present one suggestion for the preferred region in section 2.
5. Switch invokes the same region/reset transaction as the explicit region picker.
6. Keep current, Back and outside dismissal preserve region/overrides and remember suppression for that language/current-region pair.

Persist a pending suggestion before requesting a locale change so Activity recreation cannot lose it. Distinguish recreation from deliberate dismissal: restoring a dialog does not count as Keep current. Track one pending suggestion, supersede it on a newer language choice, clear it on an explicit region selection/reset and consume it once after a decision. Ordinary launches must not generate fresh suggestions. A pending choice interrupted by process death may resume once when Settings is reopened.

When Android system app-language settings change the translation, preserve the app region and evaluate the same rule when Settings next becomes foreground. Do not interrupt an active calculator, background service or widget with a dialog.

Examples:

| Action/current region | Result |
| --- | --- |
| Select English while region is India, Canada or Australia | Apply English; no suggestion |
| Select Hindi while region is US | Apply Hindi; offer IN |
| Keep US after selecting Hindi | Preserve US format/currency; remember hi/US |
| Later select Hindi again while region is US | No repeated suggestion |
| Select French while region is Canada | Apply French; keep Canadian preset and CAD |
| Select German while region is US; accept | German text, German format, EUR; clear old format/currency overrides |
| Select Spanish (Mexico) while region is Spain | Change wording; no region suggestion |

## 5. Resource and translation contract

Use native Android resources as described in https://developer.android.com/guide/topics/resources/localization.

- Keep a complete US English fallback in res/values/strings.xml. Configure English's translation option as en-US without duplicating the entire English resource file unnecessarily.
- Add locale-qualified strings.xml files for every translation option. Share Spanish base strings and add regional overrides for meaningful wording differences.
- Consolidate translatable strings, plurals and user-facing arrays currently distributed across other values files into strings.xml per locale. Preserve all resource names and remove old duplicate definitions. Leave non-text dimensions, colors and styles in their existing files.
- Translate every app-owned visible label and accessibility message: catalog/search/navigation, Settings, calculators/converters, finance/tools, validation, dialogs, units/currencies/regions, history labels, Graphy content, notifications, widgets, reports and sharing templates.
- Do not put translation dictionaries in Java/Kotlin, JSON or server responses. Platform-localized generated date words are allowed; app-authored surrounding text remains XML.
- Mark machine values and identifiers non-translatable: ISO codes, stable unit IDs, navigation IDs, preference keys, URLs, formulas, symbols and brand names. Never use translated labels to restore identity or store preferences.
- Localize country names through XML resources rather than relying on a display-name cache built in the wrong language. Preserve English search aliases.
- Use complete phrases, numbered formatting placeholders and Android plurals; do not concatenate translated sentence fragments. Preserve placeholder types, escaping and plural-category requirements.
- Use a small terminology glossary in the implementation documentation for each language's repeated calculator/finance actions. Review financial and measurement terms for meaning, not literal English resemblance.
- No broad pretense of completion through English-filled locale files: check coverage and flag untranslated app-owned text. Native wording review remains a recorded acceptance item.
- Bundle every translation for offline use; configure language packaging so switching never requires a download.
- Third-party consent/advertising UI, external websites and remotely hosted content retain their own localization capabilities. Document these boundaries without adding translation infrastructure for them.

## 6. Architecture, interfaces and persistence

Follow the current Java/XML plus Compose architecture. Expected integration owners are activity/SettingsUi.kt, helper/Preferences.java, helper/PreferredCurrency.java, shared numeric formatters, existing app/Activity entry points, and Graphy's existing content/formatting boundaries. Verify source before changing it; some documentation may lag implementation.

Introduce small shared owners under the existing helper/model boundaries, not a parallel settings architecture:

- SupportedLanguage: stable translation tag, base-language code, native-label resource and suggested region code.
- RegionDefinition: ISO region code, display-name resource, formatting-locale tag, default currency code and common-language associations.
- NumberFormatSettings: decimal/group separator, grouping enabled/pattern, displayed decimal places.
- RegionalProfile: selected region, effective formatting settings, currency, and whether formatting/currency differs from the region defaults.
- A shared resolver and a single applyRegion/resetRegionDefaults path used by all entry points.

Use the existing preferences helper as the storage boundary. Reuse the existing decimal/group/precision/currency keys for compatible meanings; add selected-region, grouping-pattern, custom-state/migration metadata and suppressed/pending suggestion state as needed. Store stable codes, not localized names or enum positions. Apply related region-reset fields in one SharedPreferences edit and publish one resolved profile to avoid transient mixed configurations.

Use AppCompatDelegate application locales as the authoritative translation selection, with the supported manifest locale configuration and pre-Android-13 persistence integration. Do not maintain a competing preferred-language key. See https://developer.android.com/guide/topics/resources/app-languages. Installed AppCompat is 1.8.0 and minSdk is 26; verify these before implementation and do not upgrade dependencies speculatively.

Honor explicitly selected system app-language settings; initialize en-US only if no previous app-language choice exists. Do not repeatedly force en-US on ordinary launches or reset the region when Android updates the application locale. If Android chooses follow-system, resolve an offered translation or English fallback without changing region. Pre-Android-13 service/Application contexts must use the appropriate locale-aware resource context; do not assume the application context automatically matches the Activity.

Do not use Locale.getDefault() to derive the selected region or currency. Build number-format symbols with Western zero digits and the region's grouping conventions, then apply custom overrides. Use API-26-supported framework/ICU facilities for secondary grouping where needed; do not add a localization dependency.

## 7. Numeric input, state and history safety

- Canonical numeric input/storage remains ASCII digits, internal dot decimal, stable operators and existing bounds. Localized formatting belongs at input/display boundaries.
- Render number/keypad digits as 0–9 even if the OS or selected language normally offers another numeral system. Numeric date/time fields and timer wheels follow this rule too.
- Show the selected decimal separator on the shared keypad; map it to the canonical decimal action. Preserve the shared key grid, semantic colors, typography and feedback.
- Normalize keyboard/pasted input using the effective format. Accept Unicode decimal digits from keyboards/paste as input only when they can be deterministically mapped to 0–9; render them as Western digits. Support Unicode minus consistently. Validate complete grouping patterns before removing group marks. Reject foreign/ambiguous separators rather than guessing.
- Decimal punctuation in scientific expressions must not be confused with argument syntax. Preserve supported expression grammar and incomplete edits. Maintain canonical-to-display cursor mapping instead of editing localized strings as though positions were identical.
- Language/region/format changes preserve canonical values, unfinished edits and navigation; never reinterpret an old display with new separators. Save reconstructible state before locale-triggered recreation.
- Keep high-precision values separate from rounded result strings. Use existing numeric snapshots for Graphy, reports and continued calculations.
- New history records must retain enough canonical expression/value and format context for reliable restoration. Inspect the current schema first; preserve legacy records and their established restoration behavior. Never guess ambiguous legacy numeric text or rewrite old history using the new locale. Where canonical data is unavailable, preserve the legacy display and report a restoration limitation rather than calculating a different value.
- Currency changes do not relabel saved money, convert amounts, request rates unnecessarily, or alter existing drafts. New scenarios use the newly preferred currency.
- Refresh cached labels, picker data, formatter instances, Compose resources, Graphy content, notification text and widgets. Running timers keep exact deadlines and action identities. Preserve notification channel IDs; update their localized names where the platform supports it.
- Persisted text may include user names and historical snapshots; do not translate user text or modify raw stored amounts.

## 8. Migration

Run an idempotent, versioned migration before first rendering the regional UI.

Fresh install: en-US, US, USD, standard US grouping/separators, four-place precision default, no custom overrides.

Upgrade: default translation en-US unless an explicit app-language choice already exists; selected region US; preserve the previous effective decimal separator, group separator, grouping behavior, decimal count and preferred currency as explicit overrides. Preserve absent-key effective defaults too, so a previous no-grouping display does not suddenly acquire grouping. If currency has never been persisted, resolve its previous effective default before switching application language and persist that value. Do not mistake a read-generated preference for evidence of a user's manual selection.

After migration, an explicit different-region selection or reset clears preserved overrides and applies the new defaults. Language changes and declining a suggestion do not. Validate corrupt/unsupported values against registries; repair only the invalid setting with a deterministic US/USD or valid-region default, without clearing unrelated data. A migration rerun must not undo later user selections.

## 9. Implementation order and review gates

1. Baseline: inspect AGENTS.md, relevant skills/docs and current source. Record git status and existing failures; do not overwrite unrelated work. Inventory resource literals, locale-dependent caches, input handlers and saved state. Verify current currency/territory and CLDR association data.
2. Core: implement registries, resolver, atomic reset, migration and focused tests before UI. Verify grouping, precision and currency persistence with known expected values.
3. Settings: add group/rows, staged format dialog, searchable regions and restorable suggestion flow. Verify language-change recreation and accepted/declined suggestions.
4. Numeric integration: route input/output through shared formatting boundaries across calculators, converters, Graphy, history and finance exports. Preserve formulas and existing scenarios.
5. Text integration: consolidate English strings, translate all locales, add plural/placeholder checks and package translations offline. Confirm translated resources are not stripped in release.
6. Runtime review: verify all tool viewports, themes, languages, font/display scales, keyboard, accessibility and timer/widget behavior. Fix actual failures within scope.
7. Handoff: update SETTINGS, CALCULATOR_SYSTEM, ARCHITECTURE, TESTING and relevant typography documentation. Record results, evidence, translation-review limits and unresolved device checks.

Do not stop after scaffolding, partial translations or unit tests alone. Do not claim visual compliance from static inspection. No publishing, signing changes or dependency upgrades are part of this task.

## 10. Verification matrix and definition of done

### Core tests

- Every translation/suggested region is valid; region currencies are supported; IDs are stable and unique.
- Fresh defaults, absent-key upgrade defaults, saved overrides, corrupt values, repeated migration and reinstall/process-restoration cases.
- Direct region change, accepted suggestion and reset all use identical transactional behavior. Same-region selection preserves overrides. Region change resets decimal places to four without changing numeric precision.
- Suggestion match/nonmatch, English with US/IN/CA/AU, French with CA, both Spanish variants, remembered dismissal, language change during a pending prompt, rotation/process death and system-settings changes.
- International/Indian grouping, every supported separator, no grouping, collision validation, negative/zero/very small/large numbers, rounding boundaries, incomplete input, Unicode input digits/minus, invalid pasted grouping and cursor restoration.
- Independently expected values such as 1234567.89 -> 1,234,567.89 for US; 12,34,567.89 for IN; 1.234.567,89 for DE. Same canonical calculation gives the same underlying result across all profiles.
- Saved finance currencies, converter rows, rate caches and legacy history remain stable. New scenarios use preferred currency. Results/history/Graphy/copy/share/reports agree.

### Resource/build tests

- Check string/plural/array coverage, duplicate keys, placeholder signatures, XML escaping, required plural quantities and accidental translated machine values.
- Confirm en-US fallback and all 14 translation choices resolve correctly; resource-qualified variants do not alter the selected region.
- Build and inspect offline release language packaging as well as debug resources. No translation SDK or extra network permission.
- Run ./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain with a compatible existing JDK. Record baseline failures separately from introduced failures.

### Runtime and visual tests

- Exercise Settings on API 26–32 and API 33+; include latest supported platform when available. Test language changes through both app and Android settings.
- All 14 translation options: resource smoke tests and actual screen review. Review standard Hindi, Cyrillic, Vietnamese diacritics, Turkish case handling, long German/Polish/French labels and Spanish variants.
- US/IN/CA/AU/DE presets plus custom formats across basic, unit/currency, age, BMI, fuel, percentage, EMI, FD/RD/SIP, Timer and shared screens. Exercise Graphy and generated exports where supported.
- Every theme at a representative viewport; constrained portrait/short-wide configurations and increased font/display scaling; focused keyboard/IME tests; Back/Up/tab navigation and process recreation with unfinished input.
- Preserve canonical keypad parity and no-scrolling primary tool workflows. Never clip controls, shrink essential text illegibly or add a scrolling fallback. Mark impossible accessibility configurations unresolved.
- TalkBack labels/selection state, readable contrast, font fallback and minimum touch targets.
- Running/paused/completed timer, notifications, widget refresh, app background/foreground and language changes without duplicate side effects.

Save logs/screenshots under ignored .artifacts/ and provide a short reproducible evidence index. Native renders complement but do not replace device checks. Translation fluency review and unavailable device checks must be explicitly marked pending, not passed.

Completion requires all app-owned translations, the complete settings/region-suggestion behavior, numeric and persistence regression coverage, successful required checks or clearly attributed external blockers, and honest runtime/translation-review status.
