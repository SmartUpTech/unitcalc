---
name: android-performance
description: Diagnose and improve Android startup, UI jank, memory, battery, conversion latency, and release size in Java/XML and Kotlin/Compose. Use for performance regressions, lifecycle leaks, slow screens, timer efficiency, or R8/resource-shrinking changes.
---

# Android Performance

1. Read AGENTS.md, docs/PERFORMANCE.md, and only the affected implementation. Identify Views/Compose, lifecycle ownership, build variant, and a reproducible user journey.
2. Establish a baseline before changing code: device/API, release/profileable build, input size, startup or frame timings, allocations, or APK/AAB size. Use Android Studio Profiler, Perfetto, or existing benchmarks as appropriate. Do not infer release performance from debug Compose builds.
3. Inspect the bottleneck:
   - Move network, database, expensive parsing, and large conversions off the main thread using existing executors/coroutines. Cancel work with its owner; debounce only where input semantics allow it.
   - Reuse list items, stable IDs/keys, and bounded caches. Avoid allocations and repeated parsing during rendering.
   - Release bindings, observers, listeners, ads, and callbacks at the correct lifecycle boundary. Do not retain Activity/View references in long-lived objects.
   - In Compose, keep expensive work outside composition, scope state reads, use lifecycle-aware collection and keyed effects. Use remember/derivedStateOf only when appropriate; never mark mutable types @Stable merely to suppress recomposition.
   - For timers, use elapsed-time deadlines rather than decrementing a counter as the source of truth; stop redundant ticks when not visible and preserve background completion.
4. For shrinking, prefer direct resource references or narrowly scoped tools:keep rules for dynamically resolved resources. Inspect reflection/JNI and R8 keep requirements. Do not disable optimization globally as the default fix; test affected dynamic paths in the optimized release build.
5. Make the smallest evidence-backed change, retain calculation accuracy and theme behavior, then rerun the same measurement. Consider app-specific Baseline Profiles for proven startup/critical-journey bottlenecks without adding a benchmark module automatically.
6. Run applicable existing unit tests/lint and release smoke checks. Report before/after evidence, tradeoffs, and unavailable profiling rather than inventing speedups.

Read [verification.md](references/verification.md) for task-specific checks and current official sources.
