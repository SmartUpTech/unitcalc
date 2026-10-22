---
name: calculator-logic
description: Core logic for mathematical evaluation, unit conversions, and the calculator registry. Use this when adding new units, fixing parsing bugs, or modifying calculation behavior.
---

# CALCULATOR SYSTEM

## EVALUATION PIPELINE
1. **Parsing**: Converts raw string input into an expression tree.
2. **Precedence**: Handles operator priority (e.g., multiplication before addition).
3. **Precision**: Ensures consistent decimal handling.
4. **Result**: Produces a `CalculationHistoryItem` for storage.

## DOMAIN MODELS
- **Unit**: Represents physical quantities (Length, Mass, etc.).
- **Currency**: Handles monetary conversions with rate caching.
- **Conversion**: Manages the state and logic of a specific conversion session.

## IMPLEMENTATION RULES
- **Reactive Calculation**: Results should update immediately as input changes (where applicable).
- **Error Handling**: Gracefully handle Division by Zero, Syntax Errors, and Overflow.
- **Features**: Support sign toggling, percentages, and scientific functions (sqrt, powers).
- **Graphy Integration**: Logic must produce structured snapshots for visual rendering.

## REGISTRY
- **CalculatorCatalog**: The central source of truth for all tools. New categories, units, or calculators MUST be registered here to appear in the UI.


## NUMERICAL CONTRACT

- Canonical calculator/keypad input uses ASCII digits, `.` and an optional leading sign;
  grouping and decimal preferences apply to display only. Editable operands retain all typed
  digits, decimal points and trailing zeros. Malformed converter input is rejected, not stripped.
- Basic calculator literals and +, -, × and percentages use exact decimal arithmetic.
  Terminating division is exact; repeating division, square roots and integer powers use
  DECIMAL128 (34 significant digits, HALF_EVEN).
  Fractional powers retain the existing finite `Math.pow` scientific approximation; they do not
  promise 34 accurate digits. Precedence and percent semantics remain unchanged.
- The evaluator, trace, display and continued calculation carry BigDecimal. The double accessors
  are compatibility/Graphy metadata boundaries, never the source for calculator continuation.
  Pressing an operator after `=` uses the canonical result; negative results are grouped before
  exponentiation. History/copy store displayed text; restoring history reevaluates its expression.
- Results are rounded at the display boundary. Nonterminating results remain approximations;
  e.g. 1/3 × 3 displays 1 at normal decimal settings, without forcing internal values to integers.
- Work is bounded: at most 1024 expression characters, 32 nested parentheses, 64 recursive
  factors, 1024 significant result digits, adjusted decimal exponent -300 through +300, and
  integer powers of magnitude at most 1000. Out-of-range/domain/divide-by-zero results are invalid,
  clear the previous result/Graphy and show the existing error message on final evaluation.
- Square root uses bounded Newton iteration with guard digits using API 26-compatible methods.
  `BigDecimal.sqrt` requires API 33, so production code must not call it directly.
- Linear units use one canonical decimal or rational base definition. Cross-multiply before
  one DECIMAL128 division; do not maintain independently rounded reciprocal factors.
- Keep the existing 365-day year / 12-month year and US vs Imperial volume distinctions.
  Existing binary storage IDs/factors/positions are retained with corrected IEC names; decimal
  SI units are appended as separate choices. This is duration conversion, not calendar-date arithmetic.
- Corrected exact definitions include bit = 1/8388608 MB, cubic yard = 0.9144³ m³,
  US teaspoon = US gallon/768, tablespoon = US gallon/256, km/h = 5/18 m/s,
  m/min = 1/60 m/s, knot = 1852/3600 m/s, torr = 101325/760 Pa.
- Temperature retains its separate affine/piecewise formulas. Fuel retains its separate
  reciprocal formulas and explicit zero convention, now using precision-based decimal division
  rather than scale-based UP rounding. Never send either through the generic linear converter.
- Currency snapshots and calculations use BigDecimal instead of float. Cache decimal rate strings
  and continue reading legacy numeric caches. Validate positive finite rates, currency identity,
  dates and freshness; failed refreshes must preserve the prior snapshot. Currency display retains
  its existing two decimal places. Same-currency conversion is identity without requiring a rate.
- Graphy consumes the formatted authoritative result and decimal calculator trace. Linear
  conversion metadata obtains its coefficient from the converter itself. Its double diagnostic
  `apply` is approximate and must not calculate the application's displayed result.

### Definition references (checked 2026-10-22)

- NIST SP 811 Appendix B.8 / B.9:
  https://www.nist.gov/pml/special-publication-811/nist-guide-si-appendix-b-conversion-factors/nist-guide-si-appendix-b8
  (tables may print rounded factors; exact definitions above derive from the international
  yard 0.9144 m, pound 0.45359237 kg, standard gravity 9.80665 m/s², and US gallon 231 in³).
- Java decimal arithmetic / rounding:
  https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html
- Android method availability:
  https://developer.android.com/reference/java/math/BigDecimal#sqrt(java.math.MathContext)


## EXPANDED UNIT REGISTRY

### Currency country coverage (2026-10-22)

- CurrencyConverterFragment defines 100 country/region entries representing 98 distinct
  Frankfurter v2 currency codes. Hong Kong is retained for compatibility. Bulgaria, Croatia
  and Germany share EUR; country names in the searchable picker distinguish these entries.
- Existing selection positions are preserved. Former BGN and HRK entries now select EUR
  for Bulgaria and Croatia. The former generic EU entry represents Germany/EUR. New
  entries are appended; do not sort this list without migrating persisted preference indices.
- Flags use direct drawable references so missing resources fail compilation and release
  shrinking retains them. Croatia uses hr (the previous cr reference was Costa Rica), and
  Germany uses de in place of the generic eu flag. No flag image files were modified.
  Dominican Republic was excluded because a do flag resource is absent; Brunei is included.
- All 98 codes were checked against https://api.frankfurter.dev/v2/rates?base=USD.
  Cached/offline rates remain subject to availability; no fallback rates are fabricated.
  Calculation precision, rate loading and refresh behavior are unchanged.
- Validation: all three CurrencyConverterFragmentTest cases passed, including drawable
  resolution for every entry, legacy selection positions, AED cross-rates, identity,
  missing-rate handling and selection restoration. The currency-filtered run passed
  11 of 12 tests; unchanged CurrencyDecimalStoreTest fails because JSONArray is not
  mocked. An unchanged GamesFragmentTest compile error required a temporary Gradle
  init-script exclusion for this run. Production sources compiled with the build cache
  disabled after stale cached resources caused linking errors. Device visual checks
  across themes, constrained viewports and font scaling remain pending (no device attached).

- Angle is category 15; currency remains 14. Category pickers iterate `Conversions.getCategoryIds()`
  instead of assuming a contiguous range ending at Currency. CalculatorCatalog exposes Angle to
  the existing navigation/favorites system; it uses the shared linear Graphy primitives.
- Angle uses degrees as its base: 1 revolution = 360 degrees, 1 gradian = 0.9 degrees,
  1 radian = 180/π degrees. The stored π has guard digits beyond DECIMAL128 precision.
  Radian conversions are approximations; signed angles and multiple turns are not normalized.
- Additional units are appended so persisted unit indices and category IDs keep their meanings.
  Storage's existing binary values now show IEC labels/symbols (KiB through EiB, Kibit through
  Eibit), and separate SI decimal choices (kB through EB, kbit through Ebit) are appended.
- Land units explicitly identify their definitions. Guntha = 1/40 acre, cent = 1/100 acre;
  Marla uses the 272.25 sq ft convention. Bigha variants are Assam (14400 sq ft), Rajasthan
  pucca/Shahjahani Jarib (27225 sq ft), Rajasthan kaccha/Gantari Jarib (17424 sq ft).
  These are named conventions, not universal state-wide/legal survey assumptions. Exact square-foot
  definitions are converted using 1 ft² = 0.09290304 m², not rounded metric figures from source tables.
- Mass adds metric quintal (100 kg), tola (180 grains) and troy ounce (480 grains).
  One grain = 0.06479891 g; tola and troy ounce must not be confused with metric gold weights or
  the existing avoirdupois ounce.
- Volume adds µL, cL, dL and the 42-US-gallon oil barrel. The existing US barrel remains 31.5 gal
  and is labelled as a beer barrel. Cooking adds a 250 mL metric cup, Australian 20 mL tablespoon,
  metric 5 mL teaspoon and 15 mL tablespoon. These same variant objects also appear under Volume.
- Imperial spoons use explicitly labelled volume conventions (teaspoon = 1/768 imperial gallon,
  tablespoon = 1/256 imperial gallon). They are not modern UK metric spoons or universal historic
  spoon sizes; no unqualified UK spoon definition is assumed. Existing US spoons/cup are labelled US.
- Pressure adds mbar (100 Pa), conventional inHg (3386.389 Pa), conventional cmH₂O (98.0665 Pa)
  and conventional inH₂O (249.08891 Pa). Temperature-specific manometer variants are not substituted.
- Torque adds kgf·m (9.80665 N·m) and N·cm (0.01 N·m).

### Added definition references (checked 2026-10-22)

- IEC/SI data prefixes and 8 bits per byte: https://physics.nist.gov/cuu/Units/binary.html
- Conventional pressure, mass, volume, angle and torque units:
  https://www.nist.gov/pml/special-publication-811/nist-guide-si-appendix-b-conversion-factors/nist-guide-si-appendix-b9
- Named land conventions (use exact stated imperial definitions, not rounded metric equivalents):
  https://docs.apisetu.gov.in/document-central/mdds/Annexure.html
- Assam bigha (14400 sq ft), Government of Assam PWD:
  https://pwdroads.assam.gov.in/sites/default/files/swf_utility_folder/departments/pwd_lipl_in_oid_3/menu/document/rap17.pdf
- Tola (180 grains), Maharashtra Gazetteer:
  https://www.gazetteers.maharashtra.gov.in/cultural.maharashtra.gov.in/english/gazetteer/KOLABA/bank_wt_measures.html
- Australian metric cooking measures, manufacturer measurement table:
  https://www.breville.com/content/dam/breville/au/assets/miscellaneous/instruction-manual/mixers/BEM800-instruction-manual.pdf
- Explicit Imperial spoon convention cross-check (converter owner's published definitions):
  https://www.unitconverters.net/volume/teaspoon-uk-to-milliliter.htm
