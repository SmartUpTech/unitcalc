# Calculator and converter accuracy verification

## Automated regression coverage

- Exact decimals: 0.1+0.2=0.3; 0.3-0.2-0.1=0; 1.005×100=100.5.
- Large integer: 9007199254740992+1=9007199254740993; exact Graphy intermediate and final values.
- Precedence, grouped operations, unary signs, percentages, reciprocal powers and scientific powers.
- √16=4, √0=0, nested/tiny roots, √2 tolerance, incomplete √ inputs and invalid negative roots.
- Partial square-root display at every typing step; trailing dot/zeros and unrounded input digits.
- Continued calculation independent of display precision; negative zero formatting; comma decimal output.
- Time, length, mass, volume, cooking, energy, torque, binary storage and speed references.
- All pairs in every linear category, six input magnitudes/signs: inverse error <= max(1,|input|)×1e-31.
- Temperature references and reciprocal fuel examples/zero behavior.
- Fixed currency fixture: 100 USD×83.25=8325 INR and reverse=100 USD; large identity values.
- Decimal provider rates/cache round trips; legacy cache reading and invalid-rate rejection.
- Robolectric additions: actual √ keypad sequence, continuation after = and saved canonical state;
  Graphy unit factors agree with corrected year/month and cubic-yard conversions.

## Executed in the task environment

- Core numeric and formatter classes compiled with Java 17. Android formatting/theme/Preferences
  references were replaced only in the standalone test classpath with minimal stubs; production
  source files were compiled unchanged.
- 40 focused JUnit tests passed using a standalone JUnit 4.10 runner, Hamcrest 1.3 and JSON-java
  (test-only fallback because the configured Gradle distribution is unavailable). This is not the
  configured Android/Robolectric suite and does not establish UI or Android JSON runtime behavior.
- Independent production-code numeric smoke harness: 11,281 checks passed, including all linear
  pair round trips and known references. Round trips supplement, not replace, reference cases.
- `git diff --check` passed.
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` could not start:
  the wrapper cannot download Gradle 9.8.0 from services.gradle.org (network unreachable).

## Required Android/device checks before release

1. Run the configured Gradle tests/build/lint, including CalculatorAccuracyTest and currency/Graphy tests.
2. API 26 and a current Android version: tap √, type 16, close/auto-close, =; backspace partial/nested
   roots; √(-1) and divide by zero must clear prior results without crashing.
3. Set two display decimals: 1÷8= shows 0.12; ×8= gives 1. Repeat using sign toggle and negative-result
   exponentiation. Rotate/recreate between = and the next operator; verify precision is retained.
4. Change decimal/grouping settings and resume. Visible expression/result/Graphy must agree, with
   no reparsing of localized display text. Verify full entered digits remain visible while typing.
5. Check 99 yr→1188 mo, reverse, 99 yd³→2673 ft³ and 8 trillion bit→1 trillion byte on screen.
6. Currency: fixed cached rate, same-currency identity, rapid selection/refresh, offline cache and
   unavailable-rate state. No stale selection may overwrite the current result.
7. Test zero, negatives where supported, fractional values, tiny/large values, repeated decimal
   keys, clear/reset, copy/history restore and typing responsiveness with long expressions.


## Angle and existing-category expansion follow-up

- Added Angle (degree, radian, gradian, revolution) and the requested storage, mass, area,
  volume, cooking, pressure and torque additions. Existing category IDs and unit positions remain
  stable; storage labels now distinguish the existing binary definitions from appended decimal units.
- Follow-up validation: 24 relevant JVM tests passed with the standalone JUnit 4.10/Hamcrest 1.3
  fallback. Tests include every added definition, decimal/binary prefixes, named regional area
  references, multi-turn/negative angles, radian tolerances, registry/catalog exposure, duplicate IDs
  and all linear-category pair round trips across six inputs, including Angle.
- Added an Android/Robolectric test for selecting Angle through the category popup, entering
  -180 degrees and converting to -0.5 revolutions, plus Angle in Graphy factor tests.
- The new drawable and string resources are XML-validated; source references and preserved unit
  ordering were checked. The configured Gradle test/build/lint attempt is still blocked by the
  Gradle 9.8.0 download restriction; Android UI/Graphy execution and visual checks remain pending.
- Device review should include long regional/cooking labels, IEC vs decimal search results,
  saved Angle selection across recreation, signed input, and shared Graphy explanations.
