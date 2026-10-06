# Calculator+ Web Games integration

## Architecture and navigation

All feature classes live in `net.smartlogic.unitconverter.games`. `GamesFragment`
owns the view lifecycle; `GamesWebView` owns transport/security; `GamesBridge`
validates the shared v1 envelope; `GamesHostConfiguration` adapts the existing
semantic theme; `GamesStateRepository` writes completion records; `GamesSession`
contains process-memory-only ad bookkeeping. Ad thresholds remain in the existing
`AdMobManager`, which owns the session instance. There is no native catalog,
game enum, game icon list, or game-specific bridge method.

`GamesConfig.ENTRY_URL` is the only production entry definition. It currently
contains the requested `#shape_fit` route. The integration adds `embedded=1`
before that hash. The landing page is the same document without a hash; the web
Back action navigates there. Changing the centralized entry hash requires no
changes to Activities, Fragments, or bridge classes.

The bottom bar is Calculate, Convert, Explore, Favourites, Play. Existing
fragments retain the existing add/hide/show behavior and state. History remains
available through its existing CalculatorCatalog drawer and Explore entries.
Its destination ID is retained as a resource, and resume/restoration handles it
even though it is no longer a bottom-bar item. Play hides the calculator-specific
favorite action. Native tab and trusted web route state are saved for recreation.

## WebView, theme and lifecycle

- Load the hosted platform, never APK assets or native puzzle implementations.
- Install AndroidX WebKit's origin-scoped `AndroidGames.postMessage` listener
  before loading. Accept messages only from the main frame, the exact HTTPS
  SmartUpTech origin (default port or 443), and the `/games/` path.
- Reject cross-origin navigation/resources, secondary-frame navigation, encoded
  traversal, user-info URLs, file/content/intent/javascript URLs, mixed content,
  third-party cookies and SSL errors. Never use an unrestricted
  `addJavascriptInterface` fallback. Unsupported WebViews get an update message.
- Send bridge/app versions, application ID, resource locale, local ISO date,
  system timezone, today's generic completion map and semantic palette.
- Reuse CalculatorTheme background for background/surface, mainText for primary
  text, functions for secondary text/dividers, equal for accent, and the existing
  light-background policy for mode. Keep the native WindowChrome/inset behavior.
- Hide the WebView behind a themed native status surface until configuration is
  accepted and a visual frame is available. Do the same during actual palette,
  locale or date changes. No native Games header, browser chrome or ScrollView.
- Call only the platform's public `SmartUpGames.configure` and `SmartUpGames.back`
  APIs. JSON serialization avoids interpolation of untrusted event values.
- Stop visible-only date checks and pause the WebView on background/tab hiding;
  resume/reconfigure if host identity changed. Ordinary tab/ad return does not
  reconfigure unchanged state. A completion-map change alone does not restart the
  next puzzle: the web session already owns its immediate pending badges.
- Back closes an open native drawer first, then returns from a hashed web route
  to the landing page; landing Back delegates to the app's normal dispatcher.
- Handle timeout, offline/main-document HTTP/SSL failure, renderer loss and
  persistence failure with a themed retry surface. Destroy the bridge/WebView
  and remove theme listeners/timers with the view lifecycle. Partial web play
  currently resets on WebView recreation; committed completions survive.

## Completion and advertisements

Only a bounded, correctly versioned completion envelope for a started game and
the current configured/local day is accepted. The durable preference key
`games_completion_records_v1` stores `date:gameId` records. Keeping previously
completed dates prevents replay after device-clock rollback. Malformed records
are ignored without changing any other preferences. Writes use one serialized
worker and `commit()` off the UI thread. Duplicate writes return DUPLICATE, and
failed writes return FAILED and restore in-memory state for retry. Raw results,
scores, user identifiers and ad counters are not persisted or uploaded.

After a NEW durable completion, the existing Ad Manager increments its Games
session count. The opportunities are exactly completion 3 and completion 8,
with at most two reservations per process session. Ads are skipped if no ad is
ready, consent is ineligible, another Games/app-open ad is showing, the Activity
is unavailable, or the original completion break is no longer visible. Route,
document and navigation generations reject stale opportunities, including
leaving and revisiting the same URL. No delayed show/reward gate exists.

GMA Next-Gen interstitial is the appropriate non-rewarded format for these
natural completion breaks. **The SDK/account controls the creative; Android
cannot guarantee a video-only interstitial.** A separate Games preload slot
preserves existing ad inventory/timing. Debug Games requests use Google's
interstitial test ID. Release reuses `am_interstitial_ad_unit`, currently `XX`,
so release Games ads remain disabled until the owner supplies a valid unit.

The new Games preload/show path checks UMP `canRequestAds()`, updates consent
when first entering Play, presents required forms only while Play is visible,
and exposes required privacy choices in the existing native overflow menu.
Changing those choices destroys Games inventory before reloading. Existing
SDK initialization, app-open and other ad flows are not migrated; inspection
found that the pre-existing app-wide UMP flow is absent. That existing privacy
gap, account messages, audience classification and mediation settings require
a separate app-wide review; this PR does not certify overall compliance.

Policy/API sources reviewed on 2026-10-06:
- https://developers.google.com/admob/android/next-gen/interstitial
- https://developers.google.com/admob/android/next-gen/privacy
- https://support.google.com/admob/answer/6201350
- https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges
- https://developer.android.com/reference/androidx/webkit/WebViewCompat

## Hosted compatibility check

The live `index.html`, `app.mjs` and `core.mjs` were downloaded and inspected.
The live platform reports version 0.0.8 and matches the existing v1 contract.

| Capability | Current behavior |
| --- | --- |
| Semantic themes | `SmartUpGames.configure(config)` applies embedded semantic tokens. |
| Locale | `language`/`locale` supplied; UI/content support English, Hindi, Marathi, with English fallback. |
| Completion state | Generic ID-to-date `completions` map; embedded mode uses native state. |
| Ready/start/complete/exit/error | Generic v1 JSON events through `AndroidGames.postMessage`. |
| Versioning | `bridgeVersion` on host config/events; catalog compatibility is web-owned. |
| Android Back | Public `SmartUpGames.back()` returns to the hashless landing page. |
| Mobile layout | Shared CSS uses available WebView viewport; host owns app bar/navigation/insets. Actual Android device layout still needs verification. |
| Runtime themes | Reconfiguration applies tokens but remounts unfinished play. |

## Required Web Games Changes

The current v1 contract can load and play. These common platform improvements
remain for complete state/UX fidelity; no Calculator+-specific DOM/CSS injection
was added to conceal them.

| Current behavior | Problem | Required web change | Why common platform |
| --- | --- | --- | --- |
| Every configure call runs leave/route and remounts the puzzle. | An actual theme/locale change restarts unfinished play. | Apply same-day palette/locale updates in place; reset only on an explicit reset, new host/day, or deliberate navigation. | Every native host needs runtime styling/localization without losing input. |
| Web completion immediately shows success and a pending badge; there is no durable-save acknowledgment. | Disk failure requires a native error/reload; the web result cannot explain or retry a failed save. | Version a generic completion acknowledgment/failed-save response, keep the pending result for retry, and distinguish saved from pending without game-specific APIs. | Durable completion semantics must be identical across SmartUpTech apps. |
| onReady is emitted before asynchronous catalog boot; configure returns before routing/assets finish. | The native loading surface can hand off to the themed web loading/retry state while content is still loading. | Keep the configuration handshake, then expose a generic content-ready/loading state after the first route renders. | All hosts need a reliable render-ready signal without inspecting DOM classes or knowing games. |

Optional follow-up: a separate theme-update API and generic route/back state
would simplify hosts, and versioned partial-play snapshots would preserve input
across WebView/process recreation. These belong in the shared platform too.

## Verification record and remaining release gates

- PASSED: 8 unmodified JUnit tests for GamesConfig/GamesSession, compiled/run on
  Java 17: exact origin/path restrictions, malformed IDs/dates, thresholds 3/8,
  two-ad cap, duplicates, no late retry, cross-day session and fresh process.
- PASSED: the 5 GamesBridge test bodies run as plain JVM JUnit with org.json
  20240303 after omitting only Robolectric/Android runner annotations in a
  temporary copy: handshake, start/finish/duplicate/exit, stale dates, bad
  versions/results/IDs, bounded messages and generic catalog errors. This is
  parser-policy coverage, not an Android/Robolectric run.
- PASSED: GamesWebView/GamesConfig compile against Android API 28 and the actual
  AndroidX WebKit 1.12.1 classes, with a temporary resource-ID declaration. This
  validates transport API usage; it is not an APK build.
- PASSED: existing shared website Node suite, 12 tests, including host/catalog
  validation, 800-day puzzle cases, corrupt storage, language fallback and the
  current Shape Fit regression. This validates shared source, not native UI.
- Added Robolectric tests for concurrent/durable/duplicate/corrupt/failed
  preference writes and all existing theme-to-host mappings; these remain
  unexecuted until the Android Gradle build can run.
- PASSED: parsed all 6 changed/new Android XML resources with the Java XML parser;
  `git diff --check` and final scope review passed.
- BLOCKED: `testDebugUnitTest lintDebug assembleDebug`. The repository's existing
  Gradle 9.7.1 was downloaded unchanged after finding no cached Android setup.
  Gradle's JVM cannot reach the dependency proxy, so resolution fails at the
  existing `com.android.application:9.3.2` plugin before app compilation. The
  wrapper, Android plugin, compile/target SDK and existing dependency versions
  were not changed. No Android SDK/emulator is installed in this workspace.
- BLOCKED: shared mobile browser suite. Playwright has no browser executable,
  and its browser download returns an unavailable/non-ZIP response. Source
  inspection is not visual verification.

Static regression review confirms calculation/conversion engines, catalog,
favorite/history repositories, Settings/themes, timer and startup implementations
are unchanged. The integration touches only destination restoration, the requested
bottom bar, Games resources and Games-specific ad handling. Existing ad methods
gain only exclusion while a Games ad is open; AppOpen gets the corresponding
exclusion and a read-only show-state accessor.

Before merging, run the full Android test/lint/debug/release checks in the
existing configured build environment, then verify Calculate, Convert, Explore,
Favourites, drawer/Explore History, Settings/theme changes, startup, timer,
existing ads, tab return/Back, process recreation, midnight/timezone changes,
offline/retry, large text/TalkBack, all app palettes and live test ad behavior on
a device. No device regression or performance measurements are claimed here.
Do not merge until those release gates and the production ad configuration are
reviewed. Rollback is to revert this integration commit; completion records are
isolated and do not modify existing preferences.
