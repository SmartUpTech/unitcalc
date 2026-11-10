# Localization data provenance

Implementation work in progress; this file is not a completion certificate.

Data retrieved on 2026-11-10:

- Unicode CLDR **release-48**, `common/supplemental/supplementalData.xml`:
  https://raw.githubusercontent.com/unicode-org/cldr/release-48/common/supplemental/supplementalData.xml
  Tag commit: `acd6d88ae493633240e19a87a721076a8a75c310`.
  Download SHA-256: `FB054C9F9A95F0C4DA9DD1E5E0EF6DD208B6F9A73F45303011BE247277D81BEF`.
- CLDR release-48 `common/supplemental/likelySubtags.xml` and `common/main/en.xml`
  supply explicit formatting-locale candidates and English country search aliases.
- SIX, the ISO 4217 maintenance agency, current currency list, publication date
  **2026-09-17**:
  https://www.six-group.com/dam/download/financial-information/data-center/iso-currrency/lists/list-one.xml
  Download SHA-256: `33139B438657D1CEE116BA737807EA71D19D6DE4B90F799A09C56F0CC6A1B0FF`.
- SIX's published Bulgaria amendment makes EUR current from 2026-01-01:
  https://www.six-group.com/en/products-services/financial-information/market-reference-data/data-standards.html

The registry filters CLDR tender entries by effective date and existing catalog
currency coverage, retaining 176 ISO territories. Non-ISO aggregate and special
CLDR territory identifiers are excluded. Reconciliation against SIX's named
entities covers 173 associations, including explicit aliases for differing country
names. Three associations need additional authority because SIX's list does not
describe these tender arrangements fully:

- GS / GBP: South Georgia and the South Sandwich Islands' Currency Ordinance
  specifies sterling: https://laws.gov.gs/wp-content/uploads/2022/07/Currency-Ordinance-2000.pdf.
- PS / ILS: the Palestine Monetary Authority's statement on the shekel cash
  surplus confirms ILS alongside USD, JOD and EUR. The registry deliberately
  chooses ILS as the preset; it does not assert exclusive tender:
  https://www.pma.ps/ar/الإعلام/بيانات-صحفية/أسئلة-واجابات-حول-أزمة-تكدس-الشيكل.
- ZW / USD: USD remains part of Zimbabwe's multicurrency arrangement. ZiG is
  outside the app's existing currency catalog; USD is the supported preset:
  https://rbz.co.zw/documents/mps/2024/2024_Monetary_Policy_Statement_at_a_Glance.pdf.

The ignored reconciliation scripts and downloaded snapshots are in
`.artifacts/localization/`.

Common-language associations use CLDR `official` and `de_facto_official` statuses.
Project overrides add English for US, IN, CA and AU. Spanish variants share the
base language `es`. Formatting tags are independent of the selected translation;
Canada explicitly uses en-CA and India uses en-IN. The `nu-latn` extension selects
Western numerals. CLDR is Unicode data under https://www.unicode.org/license.txt.
The notice is checked in at `docs/licenses/unicode/LICENSE.txt` and distributed
inside the APK as `assets/licenses/unicode.txt`.

Android integration guidance consulted:
https://developer.android.com/guide/topics/resources/app-languages
Installed source uses AppCompat 1.8.0 and minSdk 26. No dependency upgrade is needed.
AppCompat's application locales are the authoritative language selection;
pre-33 automatic persistence requires its metadata holder service. Application
contexts below API 33 need locale-aware resource handling separately from activities.
Activity initialization runs after AppCompat's `super.onCreate`; a nonempty
application-locale choice is preserved before applying any fresh-install default.
Default XML text declares English with the documented `tools:locale` metadata:
https://developer.android.com/studio/write/tool-attributes#toolslocale.

Evidence and downloaded reference snapshots are in ignored `.artifacts/localization/`.
