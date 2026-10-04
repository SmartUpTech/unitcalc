# Verification by change

- Startup: measure cold/warm starts on the same device and build; separate initialization from first useful content.
- Input/conversion: exercise rapid typing, long expressions, category switching, invalid input, and cached/offline currency behavior; preserve precision and rounding contracts.
- Lists/Graphy: measure scrolling and graph interactions with representative data; check allocations, recompositions, and frame time.
- Lifecycle: rotate/recreate where supported, navigate away/back, background/foreground, and inspect retained screens/listeners.
- Timer: test pause/resume, background, process recreation, expiry and recents; avoid a second independent ticking source.
- Shrinking: build an optimized release variant, trigger dynamically selected icons/resources, and inspect size and mapping before/after.

Choose tasks from the existing Gradle project; do not assume an Android SDK, signing keys, emulator, or benchmark setup is present. State missing prerequisites. Never commit credentials or generated profiling output by default.

## Official sources

Verify APIs against the project's installed versions before implementation.

- Performance overview: https://developer.android.com/topic/performance/overview
- Compose performance: https://developer.android.com/develop/ui/compose/performance
- Startup: https://developer.android.com/topic/performance/appstartup/best-practices
- Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/overview
- Resource retention: https://developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep
