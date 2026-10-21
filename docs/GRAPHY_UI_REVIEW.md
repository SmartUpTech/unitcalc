# Graphy UI change: review brief

## Business requirements

- Use the selected compact vertical design across Calculator, Unit and Currency Graphy.
- Keep input and intermediate numbers regular; only final-result numbers are bold.
- Inputs have solid outlines; intermediate values have one dashed outline and a quiet
  fill. Do not print Intermediate inside a node.
- Bound graph width. Long expressions/values grow downward, with vertical scrolling
  explicitly authorized for Graphy only. Keep app bars, workspace tabs and navigation.
- Remove permanent instructional prose; retain full details through node selection
  and the information button. Currency freshness notices remain visible initially.
- Preserve calculation results, precision, formulas, dependencies and accessibility.

## Implementation boundaries

- `GraphyNodeContent` measures and draws identical wrapped text; shared resources
  bound widths/padding/gaps, without reducing font sizes or 48dp node minimums.
- `GraphySemanticStyle` and `GraphyViewTheme` own fills, contrasting borders and dash
  tokens. The Canvas renderer and Compose node guide use the same role contract.
- `FlowchartGraphView` draws one dashed border for DERIVED nodes and resets paint
  effects before other nodes/focus. It bounds native drawing so Compose sibling
  content is not erased. Role/formula/contributor details remain available.
- `GraphLayoutEngine` preserves declared operand order within each layer so a
  derived left operand is not visually swapped with a later constant/input.
- `GraphyPanel` uses a fixed-width vertical scroll container and intrinsic graph
  height. Only spare space is weighted. New output resets the previous scroll.
- `ExpressionGraphBuilder` adds optional context metadata only; evaluator values,
  formatted results, conversion definitions and graph edges are unchanged.

## Validation coverage

Automated regressions cover typography/caption visibility, dashed versus solid
rendered borders, theme contrast, native Canvas bounds, operand order, long wrapped
values, non-overlap, minimum API 26, accessible node roles/actions and vertical-only
scrolling at 320x400 with 1x/2x font sizes in light/dark. Existing renderer coverage
also generates six graph examples across every app theme. The full app suite passed
170 tests across 34 classes. A focused Graphy suite also passed after removing three
unused Graphy strings. The existing text-layout strategy is retained. Final APK build
and lint results are reported in the task completion message.

Device checks on Mi A2 / Android 10 cover `(12+8)×3−15 = 45`,
`(12+8)×(3+2) = 100`, `((12+8)×3−15)/5+7 = 16`, intermediate details, the guide,
vertical access to final results, Unit conversion and Currency freshness context.
Saved device screenshots live in `screenshots/graphy-implemented/`; native renderer
and Compose previews are in `app/build/reports/graphy/` and
`app/build/reports/graphy-panel/` and are
not device screenshots. Automated accessibility checks do not constitute a manual
TalkBack session. Unbounded expression/translation combinations cannot be exhaustively
certified; the shared model/layout has no expression-specific drawing branches.

## Review scope

The working tree also contains previously requested Oswald theme, empty Graphy
navigation, and lint/Back fixes. They predate this UI implementation. No external
review model was invoked; this brief records requirements and verification for the
user's planned independent code review.
