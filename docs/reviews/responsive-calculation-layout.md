# Responsive tool-screen review

Status: updated implementation in draft PR #43; runtime acceptance is still blocked. No device certification is claimed.

## Problems found

- Unit and currency converters put the complete workflow in NestedScrollView. Stacked selectors, values, swap rows and intrinsic keypad heights exceeded short viewports.
- The calculator's weighted keypad could shrink below usable touch targets.
- Converter and calculator keys used different base styles and icon padding.
- Timer allocated half its screen to recents even when empty; its fixed ring and wheel heights competed with primary controls.
- Graphy is already a separate tab, so it does not need to consume primary input space.

## Implemented strategy

The existing Java/XML calculator and converters retain their framework. CalculationViewport is now a fixed FrameLayout, not a scrolling host. It supplies the height allocated after activity/workspace chrome to CalculationLayout.

The layout measures controls and keys, compacts structural whitespace, uses side-by-side panes when wide enough, and then tries inline selector/value rows only when labels and selector targets fit. It reduces preferred keypad rows to a font-aware minimum of at least 48dp only after those adaptations. Remaining height is distributed equally. Key width is checked against touch and text requirements.

Swap sits beside conversion fields. Compact currency selectors retain flags and ISO codes; full names remain available in selection dialogs and existing accessible descriptions. Unit labels may ellipsize in the selected row, with full names in the picker. Existing horizontally pannable numeric fields retain their complete values; this change removes vertical workflow scrolling, not text-field editing gestures or selection lists.

Calculator and converter number/function/operator/icon styles now share canonical parents, icon padding, minimum targets and foreground feedback. Their outer keypad padding is shared. ThemeApplier remains the semantic color owner; only the space-warning text was added to its existing text role.

Timer recents move to an explicit dialog with previous/next controls, so history no longer takes permanent space away from the primary tool. Setting wheels use the allocated space for one, three or five rows; unit labels are separate from numbers. Running timer decoration scales within remaining space while actions stay reserved. Small timer actions now have 48dp targets. Timer's bounded input wheels remain gesture controls, not scrolling content pages.

## Explicit unresolved limit

A viewport that cannot contain readable controls and accessible keys is not a supported complete workflow. Calculator/converter hosts display an explicit expand-window/rotate message while keeping the existing input view and state; they do not expose clipped interactive overflow or add a scrolling fallback. Growing the viewport restores the input surface. Timer has a conservative font-aware space guard.

This message is graceful failure, NOT satisfaction of the full-workflow acceptance criterion. The exact usability boundary, especially at large font/display scaling, needs rendered tests. A persistent small window that cannot fit remains unresolved. Do not describe every configuration as working.

## Scope and logic

Changed: basic calculator pane; shared unit converter covering all fourteen categories; currency converter; shared viewport/layout and keypad resources; selected unit labels; timer setting/running/completed/recents presentation; regression tests.

Calculation, conversion, currency-provider, Graphy and timer state-machine/business logic are unchanged. Existing app destinations and theme architecture remain unchanged. Timer's secondary-history presentation changed deliberately to prioritize its primary workflow.

AGENTS.md, DESIGN_SYSTEM.md and the UI/UX and Android theme skills contain the mandatory single-screen and shared-keypad rules.

## Validation actually performed

- PASS: changed resource XML parses; existing layout control IDs preserved; git diff whitespace checks.
- PASS: CalculationLayout and CalculationViewport compile against the installed Android 37 API using temporary resource declarations. This is a limited Java/API check, not Android resource linking or an application build.
- BLOCKED: focused Gradle test attempt on 8 October 2026 failed during project configuration because Android Gradle plugin transitive dependencies are absent from the offline cache. No tests ran and Kotlin/Compose compilation was not reached.
- No emulator, physical-device, screenshot, TalkBack, all-theme, OEM or system-inset validation completed.

Added but NOT executed: Robolectric measurements for content areas 320x400, 360x460, 412x600, 600x340 and 840x700; all fourteen unit categories; long values/labels; overlap and touch-target checks; repeated resizing; 200% font at 320x240 with explicit limit and recovery at 840x1100; narrow-width rejection; shared key typography/padding/feedback. Normal-workspace tests reject the warning state, avoiding a false pass merely because scrolling is absent. These are post-chrome content sizes, not full device resolutions.

## Required before approval

Run:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest --tests '*CalculationLayoutTest'
```

Then render calculator, all converter categories and timer phases across small/standard/large phones and foldable/multi-window sizes, 1x/1.3x/2x font and display scaling, every app theme, both navigation modes and supported API levels/OEM devices. Check actual insets, long numeric text, full picker labels, swap/reset, timer wheels and recents paging, theme switching, resize recovery and input retention. Verify IME label editing on timer and latest-Android edge-to-edge behavior. Add timer Compose bounds/interaction coverage; the current automated additions cover the View layouts only.

Keep the PR draft and unmerged until those checks pass and unsupported configurations are explicitly accepted or redesigned.
