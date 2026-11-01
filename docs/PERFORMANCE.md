---
name: performance
description: Guidelines for maintaining app speed, responsiveness, and resource efficiency. Use this when optimizing code or fixing lag/jank.
---

# PERFORMANCE GUIDELINES

## UI OPTIMIZATION
- **View Hierarchy**: Maintain shallow XML trees. Avoid unnecessary nesting.
- **ThemeApplier**: Target specific view roots rather than the entire window where possible.
- **Compose**: Use `remember` and `@Stable` annotations to prevent redundant recompositions.

## LOGIC & RESOURCE
- **Threading**: Offload heavy parsing and calculations to background threads.
- **Allocation**: Avoid frequent object creation in render loops or frequently called methods.
- **Persistence**: Batch Preference updates.
- **Cleanup**: Ensure lifecycle-aware cleanup in `onDestroyView`.

## PROTOCOL
1. **Profile**: Use Android Studio Profiler to identify bottlenecks.
2. **Measure**: Quantify the impact before and after optimization.
3. **Prioritize**: Readability and maintainability should not be sacrificed for negligible gains.

