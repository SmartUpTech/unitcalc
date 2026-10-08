# Responsive calculator and converter layout review

Status: implementation proposed; runtime acceptance remains pending. Do not treat this review as device certification.

## Findings and scope

- Both converter screens nested their complete input/result/keypad workflow in NestedScrollView, using intrinsic-height TableRows and a wrap-content weighted keypad. Large stacked fields, vertical swap rows and whitespace made normal operation scroll.
- The basic calculator used six weighted keypad rows without a font-aware minimum, allowing keys to become too small when the display consumed the viewport.
- CalculatorCatalog currently implements one basic calculator, fourteen unit categories sharing UnitConverterFragment, and CurrencyConverterFragment. Timer is outside this task. Graphy is already a separate workspace tab and does not compete with the input keypad.
- The activity reserves bottom-navigation space; workspace tabs and the mode selector sit above the allocated input viewport. Existing window/inset handling and IME suppression are preserved. Physical system-bar behavior still needs device verification.

## Proposed behavior

CalculationViewport passes its actual available height to CalculationLayout. The layout measures controls plus keypad, progressively removing structural vertical whitespace, using side-by-side panes when each pane has sufficient width, using inline selector/value rows where practical, and finally reducing preferred 56dp keypad rows to a 48dp or font-height minimum. Spare height goes to equal keypad rows. No numeric text scaling is added.

Swap sits beside the amount sections. Currency compact rows retain flags and ISO codes; full names remain in selection dialogs and existing accessible selector descriptions. Inline currency rows are used only if their measured labels, ISO text and fixed icons fit. Long selected unit labels ellipsize; dropdown rows retain the full text. Existing horizontally scrollable value fields retain the full underlying values.

When controls plus accessible keys physically exceed the viewport, overflow remains scrollable instead of clipping or reducing targets further. This includes some very short windows or high font/display scaling. A resize restores the appropriate layout. This exception requires runtime review, especially on small phones with large fonts.

## Changed components

- Shared CalculationViewport and CalculationLayout View classes.
- Basic calculator pane, unit and currency converter layouts.
- Shared converter keypad row allocation and minimum heights.
- Selected-unit label and one shared pane-width dimension.

The app currently uses XML Views for these screens, so the existing framework and theme components are preserved. No calculation, conversion, currency provider, Graphy, navigation, preference or theme-engine logic was changed.

## Validation

Passed: XML parsing, preservation of existing control IDs, git diff whitespace checks, scoped source review.

Added, but NOT executed: CalculationLayoutTest with actual Android measurement assertions for post-chrome workspaces of 320x400, 360x460, 412x600, 600x340 and 840x700; all fourteen unit categories; long values; repeated resize; 200% font fallback at 320x240 and recovery at 840x1100. These are content-area sizes, not full device resolutions.

Blocked: Gradle dependency resolution did not reach compilation in this environment. An independent attempt with the installed aapt2 exited 139. No successful build or test result is available.

Before marking ready, run:

```
./gradlew :app:assembleDebug :app:testDebugUnitTest --tests '*CalculationLayoutTest'
```

Then inspect real app screens with all themes, 1x/1.3x/2x font scales and display scaling, gesture and 3-button navigation, long labels and values, TalkBack, and supported API/OEM devices. Verify primary control bounds and text legibility, input/result continuity, selectors/swap/reset, theme changes with input present, and switching Graphy tabs. In particular, confirm the window's edge-to-edge behavior on the latest supported Android release. None of these device checks has been performed here.

## Follow-up source verification (8 October)

- Added a width guard for inline unit selectors, including scaled/localized From/To labels and a 48dp selector target. The previous guard covered currencies only.
- Invalidate the content measurement when the supplied viewport height changes, so height-only window changes cannot depend on ScrollView's measurement-cache behavior.
- Strengthened regression coverage for height-only resize, long scaled selector labels and non-overlapping primary controls. Test inflation now uses MaterialComponentsViewInflater, matching activity widget substitution rather than measuring framework Buttons.
- Passed XML/control-ID checks and git diff checks again. CalculationLayout also compiles against the installed Android 37 platform API with temporary resource-ID declarations. This limited Java/API check is not an application build, resource-link check, or runtime test.
- Retried the focused Gradle tests offline: failed during dependency resolution (uncached Android-plugin transitive dependencies), before compilation/test execution. The PR remains draft, with device and runtime acceptance outstanding.
