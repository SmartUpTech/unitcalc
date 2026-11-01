---
name: android-development
description: Implement and review Android features in this Java/XML and Kotlin/Compose app. Use for feature work, lifecycle/state changes, navigation, persistence, asynchronous work, or cross-screen integration.
---

# Android Development

1. Read AGENTS.md, docs/ARCHITECTURE.md and only task-relevant files. Confirm actual framework, installed dependencies and entry points. Preserve existing Java/XML, Compose interoperability and ViewPager2/TabLayout navigation unless migration is explicitly requested. Do not add a parallel architecture or upgrade dependencies speculatively.
2. Keep UI rendering separate from calculation and data access. Reuse existing models/helpers and preference/storage APIs. Use a single authoritative state per feature, lifecycle-aware observation, and immutable snapshots at asynchronous boundaries. Avoid introducing ViewModels or repositories solely for a small unrelated change; use existing state holders or the project's chosen pattern.
3. Keep input/selection/scroll state across navigation, configuration changes and supported process recreation. Use saved state for small reconstructible UI state and durable storage for committed history/preferences. Do not persist Activity/View references or large graphs in saved state. Version persistence changes and preserve existing user data.
4. Plan navigation before implementation: entry points, destinations, selected tab, Back versus Up, keyboard dismissal and return state. Reuse route/destination identifiers and existing navigation components. Prevent duplicate destinations on rapid taps, respect system Back/predictive Back with supported APIs, and restore top-level tab state. Validate deep-link/widget arguments and build the expected back stack. Never trap users behind an ad or loading state.
5. For slow work, use existing bounded executors/coroutine dispatchers, cancellation, timeouts and explicit loading/error/offline states. Never block the main thread with I/O, locks, sleeps or synchronous waits. A View.post callback still runs on the UI thread. Reject obsolete results when input, units, currency or screen changes; cancellation alone may not stop every callback.
6. Reuse resources, semantic theme tokens and shared components. Read [android-theme-consistency](../android-theme-consistency/SKILL.md) for visual changes, [calculator-accuracy](../calculator-accuracy/SKILL.md) for numeric changes, and [android-performance](../android-performance/SKILL.md) for latency or resource changes. Load policy skills only for their affected concerns.
7. Verify the changed journey: normal input, rapid repeated actions, validation/error, offline if applicable, Back/Up/tab switches, rotation/recreation, background return and process restoration. For UI changes also check light/dark, large text, TalkBack, insets and supported languages. Run existing relevant Gradle tests/lint; add focused regression tests for changed behavior, not tests that merely mirror implementation.
8. Report exact checks and results, unavailable device/build checks, compatibility or migration risks and remaining issues. Do not claim production readiness from static review alone. For release-sensitive changes, define a rollback path and use existing staged rollout/crash/ANR monitoring processes; publishing is a separate task.

Verify API guidance against installed versions:
- https://developer.android.com/topic/architecture/recommendations
- https://developer.android.com/guide/navigation/principles
- https://developer.android.com/topic/libraries/architecture/saving-states
