---
name: debugging
description: Guidelines for logging, tracing, and troubleshooting project-specific issues.
---

# DEBUGGING GUIDE

## CALCULATION ERRORS
- **Tracing**: Use `Log.d("CalcLogic", ...)` in the evaluation pipeline.
- **Precision**: If results are slightly off, check the `BigDecimal` scale settings.

## THEME PROPAGATION ISSUES
- **ThemeApplier**: If a View isn't changing color, check if it's being skipped by the recursive loop or if its background is being overridden by a hardcoded XML attribute.
- **Registry**: Ensure the active theme ID exists in `CalculatorThemes`.

## UI JANK
- **Compose**: Use the Layout Inspector to check for excessive recompositions in the Graphy layer.
- **Main Thread**: Check for long-running operations using the CPU Profiler.

## PREFERENCE ISSUES
- **Sync**: Verify that `Preferences.java` keys match those used in `SettingsFragment`.
- **Commit vs Apply**: Ensure `apply()` is used for non-blocking writes.
