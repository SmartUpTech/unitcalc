---
name: testing
description: Guidelines for writing and running unit and UI tests. Use this when adding new tests or verifying changes via the test suite.
---

# TESTING STRATEGY

## CATEGORIES
- **Unit Tests (`src/test`)**: Business logic, mathematical models, and utility classes.
- **UI Tests (`src/androidTest`)**: Fragment interactions, navigation flows, and theme propagation.

## TARGETS
- **Models**: Validate `Conversion` and `Unit` logic.
- **Calculator**: Verify parsing accuracy and edge case handling.
- **Catalog**: Ensure all entries are correctly registered and localized.

## BEST PRACTICES
- **Framework**: Use JUnit 4 and Mockito for dependency isolation.
- **Isolation**: No network or side effects in unit tests.
- **Execution**: Run `./gradlew test` for local verification.

## BUILD AND REFACTOR CHECKS
- Windows: set `JAVA_HOME` to an existing compatible JDK, then run `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`.
- Source groups now live under `calculator/basic`, `calculator/finance/{emi,investment}`, `calculator/tools/{age,bmi,fuel,percentage,timer}` and `converter/{unit,currency}`; shared converter classes live directly in `converter`.
- Tests of package-private APIs must declare the corresponding current production package, even where their physical test folders retain historical names. Use the declared package for Gradle `--tests` filters. Refactor test imports, package declarations and reflection strings together; preserve assertions.
- FD/RD/SIP share `InvestmentCalculatorTest`, `InvestmentFragmentTest` and `InvestmentLayoutTest` in `net.smartlogic.unitconverter.calculator.finance.investment`.
- `activity/ToolNavigationTest` covers every main tool, system/toolbar Back, cross-finance/history trails, recreation, start-up choices and stable analytics destination identities. `helper/UsageAnalyticsTest` validates bounded tool/family dimensions and excludes general menu actions and numeric inputs. `EmiFragmentTest` and inherited investment fragment checks cover result/keypad replacement, required-input invalidation, sharing and secondary actions. `InvestmentLayoutTest` checks paired editor alignment across themes/font scales; `ToolReviewRegressionTest` checks fuel-result readability and padding surviving repeated theme application.
- Device smoke tests should exercise real entry points, numeric edits, Done/Back, invalid input, schedule/Graphy tabs and return navigation. Mix light/dark palettes, button styles/shapes and calculators. Record screenshots with settings, device size/density/font scale and the input/result checked.
- Press actual inflated keypad views in integration tests. Calling a calculator's key handler directly does not verify embedded-fragment attachment or click routing. In particular, Fuel/Percentage workspace panes must inflate with their owner's child-fragment factory.
- Check constrained viewports and larger font/display scale. Report space-constraint states, clipping and inaccessible controls as unresolved, not passes. Catalogs/settings/secondary pickers may scroll; primary tool workflows may not.
- Save local logs, screenshots and UI dumps under ignored `.artifacts/`; keep a concise evidence report for reproducibility. Native Robolectric renders complement, but do not replace, device verification.

