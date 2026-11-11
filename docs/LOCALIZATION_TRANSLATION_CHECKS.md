# Offline translation checks

All 1,308 app-owned translatable resource entries are present for the 35 offered
locale choices. The 12 non-English bases are German, Hindi, Russian, Portuguese,
Indonesian (`values-in`, language tag `id`), Italian, French, Spanish, Vietnamese,
Polish, Dutch and Turkish. Regional variants inherit their language base and keep
their wording overrides. Translations, plurals and aliases live in native Android
`strings.xml` files; no translation service or runtime translation dependency is used.

Coverage includes tools, Graphy explanations, accessibility labels, history,
sharing/report templates, validation messages, units and store descriptions, in
addition to Settings. Eight visible finance/unit resources previously marked
`translatable="false"` now participate in translation checks. Preference identities,
formula execution, unit factors, rates and persisted numeric values are unchanged.

Run the complete audit and its regression checks from the repository root:

```text
python scripts/audit_localization.py --output .artifacts/localization/resources.json
python scripts/test_audit_localization.py
```

The audit checks missing entries, aliases (including missing targets/cycles),
format arguments, array lengths and plural categories. Identical text remains a
review list: unit names, currency/territory names, symbols, brands and familiar
loanwords can legitimately match English. Do not fill gaps with English copies or
exclude visible text from translation to make the audit pass.

`ToolLanguageResourcesTest` exercises Android resource selection across all offered
locales, report formatting and independent Russian/Polish plural examples.
`SettingsLanguageResourcesTest` covers Settings and regional inheritance.
Literal-percent labels use `formatted="false"`. The `age_year_unit` plural alone
ignores `ImpliedQuantity`: its count is deliberately rendered in the adjacent
`age_years` view by `AgeCalculatorFragment`, so adding another count would duplicate it.

Latest build, lint, resource, packaging and device evidence is kept under ignored
`.artifacts/localization/`. Coverage and successful compilation do not establish
native-speaker fluency or full visual/accessibility compliance. Native translation
review and the complete theme, scaling, TalkBack and device matrix remain separate
validation work; report only the device checks actually performed.

Android lint currently reports one `UnusedTranslation` warning for the legacy
`values-in` qualifier versus canonical `id` in `localeConfig`. The audit normalizes
this Android alias; Indonesian resource selection also passed the physical-device
journey. Do not add a duplicate Indonesian language option to silence that warning.
