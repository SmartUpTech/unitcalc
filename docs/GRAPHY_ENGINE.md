# Graphy Engine

The calculation determines **what** Graphy explains. The engine determines **how**.

## Shared pipeline

- Calculator: `CalculationSnapshot` + evaluator `EvalTrace` → `GraphyBridge` →
  `ExpressionGraphBuilder` → `GraphyOutput`.
- Converter: existing numeric result + formatted display + unit/rate identity →
  `ConversionGraphAdapter` → `ConversionSnapshot` + `ConversionTransformation` →
  `ConversionGraphBuilder` → `GraphyOutput`.
- Both: `GraphyPanelController` → Compose `GraphyPanel` → `FlowchartGraphView`.
  `FlowchartRenderer` validates support for workspace navigation and other hosts.

`GraphyOutput` is the renderer-neutral definition: immutable nodes, connections,
explanation and metadata. `GraphyNode` carries type, label, formatted value, semantic
role, optional formula and description. Never put colors, padding, sizes or layout
choices in a calculator/converter definition.

## Common primitives and layout

`GraphyNodeType` owns input, constant, operation, derived, result, decision,
explanation and visualization semantics. `GraphyNodeContent` owns wrapping caption
and value typography. `FlowchartGraphView` owns data cards, compact operation pills,
result emphasis, selection/focus and connectors. `GraphyPanel` owns the common
Compose node-detail dialog; the Canvas View dispatches node selection. There is one
result node; do not add a second result card above the graph.

`GraphyTopology` validates ids/edges/cycles and orders dependencies.
`GraphLayoutEngine` places dependency layers with responsive rows; it has no screen,
calculator, conversion-category or operator-specific layout branches. Linear,
merge, branch and multi-stage definitions use the same rules. Long values wrap
and increase height; the graph does not shrink fonts or require horizontal scrolling.
`OrthogonalConnectorRouter` remains the shared routing implementation.

## Transformations and accuracy

`ConversionTransformation` supports linear, affine, rate-based, reciprocal,
composite and direct mapping, with immutable scale/offset/inverse steps. Inversion
reverses step order. Its `apply` method is for validation/tests; it never supplies
the app's result. The result node consumes the exact formatted converter output.

Linear adapters reuse registry factors (including the existing digital-unit rules).
Affine temperature metadata comes from the established temperature function at
0 and 100, avoiding a second formula table. Gas Mark uses an explicitly piecewise
direct mapping. Fuel explains inverse relationships while retaining existing
converter rounding and its special zero behavior. Currency uses a **derived exchange
rate**, never a permanent constant, and carries rate, currencies and the existing
cached-rate timestamp. Unknown times and potentially outdated cached rates are
explained. No network request is made by Graphy.

`NumberUtils.formatCalculator` is shared by calculator and expression intermediates;
root results always use the snapshot's authoritative formatted text. Factor display
retains significant digits and scientific notation for tiny/large coefficients.
Important supporting context uses `GraphyOutput.METADATA_NOTICE` (localized prose)
and remains visible at first glance, including currency cache age.
Invalid/non-finite snapshots and malformed graphs produce an empty standard state;
invalid updates clear prior graph content.

## Design system, exploration and accessibility

`GraphySemanticStyle` resolves every semantic foreground/background pair through
`CalculatorTheme`, with readable contrast across all app themes. Canvas and Compose
use this same resolver. `GraphyViewTheme` reads canonical Android dimensions/font;
Compose spacing and shapes read the same resources. Graph-only fixed palettes,
shadows and duplicate token wrappers have been removed.

Nodes expose visible captions, role/value/contributor announcements and at least
48dp targets at supported widths. Tap or activate a TalkBack/keyboard node to inspect
formula, explanation and contributing values. Supporting formula details use
progressive disclosure. Theme/locale/font-scale changes refresh the existing View;
unchanged output/configuration does not rebuild graph layout. No decorative motion.
All prose and formatting templates belong in Android string resources.

## Adding Graphy

**New calculators and converters MUST use existing Graphy primitives. A new visual
primitive may only be introduced when the existing component set cannot represent
a genuinely new semantic concept.** A different appearance is not a new concept.

Provide semantic content and dependencies, reuse validated calculation state, pass
formatted values and localized labels, then use the common renderer. Never select a
renderer by calculator id. No chart implementations currently exist in v11; add a
shared chart primitive/token contract when real chart data is introduced, rather
than shipping speculative unused components.

## Verification

Run `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
`GraphyRenderingTest` exercises wrapping, branching and accessible node selection and writes
native renderer previews for six examples × every app theme to
`app/build/reports/graphy/`. These are renderer previews, not device screenshots of
the complete workspace. Device navigation, large-font scrolling, TalkBack gestures
and Compose dialog behavior still require device/emulator QA.
See [audit and scope](GRAPHY_AUDIT.md) and [verification results](GRAPHY_VERIFICATION.md).
