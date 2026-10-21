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
- Keep the existing 365-day year / 12-month year, binary storage naming convention, and US vs
  Imperial volume distinctions. This is duration conversion, not calendar-date arithmetic.
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
