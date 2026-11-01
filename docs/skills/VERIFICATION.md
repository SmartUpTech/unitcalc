---
name: verification
description: Standardized checklist for verifying changes before submission. Use this to ensure quality and consistency.
---

# VERIFICATION CHECKLIST

## 1. BUILD & STATIC ANALYSIS
- `[ ]` Run `./gradlew assembleDebug` to ensure successful compilation.
- `[ ]` Check for lint warnings in modified files.

## 2. FUNCTIONAL TESTING
- `[ ]` Run unit tests: `./gradlew test`.
- `[ ]` Verify the specific feature logic (e.g., calculation accuracy, preference persistence).

## 3. UI & THEMING
- `[ ]` Verify layout consistency with the `design-system` skill.
- `[ ]` Switch themes (Light/Dark/Custom) and ensure `ThemeApplier` propagates correctly.
- `[ ]` Check accessibility: all buttons have `contentDescription`.

## 4. LOGCAT VERIFICATION
- `[ ]` Monitor for exceptions or error logs during execution.
- `[ ]` Recommended filter: `package:net.smartlogic.unitconverter`.

## 5. AGENT ETIQUETTE
- `[ ]` Ensure no hardcoded strings or hex colors.
- `[ ]` Verify that changes are scoped to the requested task.
