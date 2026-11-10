# Localization data provenance

Implementation work in progress; this file is not a completion certificate.

Data retrieved on 2026-11-10:

- Unicode CLDR **release-48**, `common/supplemental/supplementalData.xml`:
  https://raw.githubusercontent.com/unicode-org/cldr/release-48/common/supplemental/supplementalData.xml
- CLDR release-48 `common/supplemental/likelySubtags.xml` and `common/main/en.xml`
  supply explicit formatting-locale candidates and English country search aliases.
- SIX, the ISO 4217 maintenance agency, current currency list, publication date
  **2026-09-17**:
  https://www.six-group.com/dam/download/financial-information/data-center/iso-currrency/lists/list-one.xml
- SIX's published Bulgaria amendment makes EUR current from 2026-01-01:
  https://www.six-group.com/en/products-services/financial-information/market-reference-data/data-standards.html

The registry filters CLDR tender entries by effective date and existing catalog
currency coverage. Individual territory associations still require a reconciliation
against SIX's named entities; checking that a currency exists in SIX is insufficient
to establish every territory association. This reconciliation is pending.

Common-language associations use CLDR `official` and `de_facto_official` statuses.
Project overrides add English for US, IN, CA and AU. Spanish variants share the
base language `es`. Formatting tags are independent of the selected translation;
Canada explicitly uses en-CA and India uses en-IN. The `nu-latn` extension selects
Western numerals. CLDR is Unicode data under https://www.unicode.org/license.txt.

Android integration guidance consulted:
https://developer.android.com/guide/topics/resources/app-languages
Installed source uses AppCompat 1.8.0 and minSdk 26. No dependency upgrade is needed.
AppCompat's application locales are the authoritative language selection;
pre-33 automatic persistence requires its metadata holder service. Application
contexts below API 33 need locale-aware resource handling separately from activities.

Evidence and downloaded reference snapshots are in ignored `.artifacts/localization/`.
