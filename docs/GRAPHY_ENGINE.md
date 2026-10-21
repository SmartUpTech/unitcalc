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

Inputs/constants have quiet solid outlines. Derived values have a single dashed
outline and light semantic fill; do not print a generic Intermediate label inside.
Only final-result values are bold. Generic role captions are reserved for node
details and accessibility; meaningful captions such as Exchange rate remain visible.
Node widths are bounded by shared resources and long text wraps without reducing
font sizes. Role colors, border contrast and dash tokens are shared with the guide.
Default value boxes and operator circles have a 32dp visual minimum; operators use
20sp type and value boxes use 2dp vertical padding. Text scaling grows shapes as
needed. Separate 48dp touch/accessibility bounds preserve usable targets without
adding visible bulk. Connectors attach to visible borders with continuous shafts
and destination arrowheads.
Native drawing is bounded to the View so it cannot erase surrounding Compose UI.

`GraphyTopology` validates ids/edges/cycles and orders dependencies.
`GraphStagePlan` groups operations by dependency depth. A stage can contain several
independent calculations; a later stage consumes their outputs. Stage numbers are
stable when folded. Inputs are introduced immediately before their first consumer.
Every original semantic node appears once: an intermediate is both the output of
one calculation and an operand of the next. Equal display values in separate
branches are never merged. No evaluation, rounding or formatting occurs here.

`GraphLayoutEngine` measures content with shared typography and dynamic box sizes,
keeps original operand order, and lays out stages downward. Rows reserve 48dp hit
areas and expand horizontally when needed. `OrthogonalConnectorRouter` owns shared
border anchors, continuous shafts, arrowheads and obstacle bypass lanes. There are
no calculator-, converter-, category- or operator-specific layout branches.

IDE-style controls in the left gutter fold each stage without cards around it.
Folding hides its operation nodes and exclusive original inputs, retains produced
values and contracts hidden paths into edges between visible boundary nodes.
Previous-stage outputs remain connected even when a later operation is folded.
Node details always resolve to the unchanged original graph and its dependencies.
Fold controls expose state and expand/collapse actions to accessibility and keyboard
users. Native Canvas drags delegate pixel deltas to the two Compose scroll states,
with touch slop, velocity-based flinging and cancellation of node clicks while
dragging. This avoids workspace-pager interception and native/Compose touch conflicts. The Compose host saves the folded-stage set for the current calculation.

The user explicitly authorized vertical scrolling and subsequently horizontal
scrolling for Graphy on 2026-10-21. Both are exceptions to the general single-screen
tool rule. The graph keeps readable text and expands horizontally for wider stages,
vertically for more stages and wrapped content. Calculator/converter input screens
retain the non-scrolling requirement. Short graphs center in spare vertical space;
only spacers carry weight, never the intrinsic-height graph.
New graph output resets the previous scroll offset. The panel shows optional
`METADATA_CONTEXT` content, the graph and important notices, with explanations and
the node guide available through the information button instead of permanent prose.

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

Empty Graphy workspace pages remain disabled. The shared `WorkspaceTabController`
coalesces rejected tap/swipe selections and restores the last enabled page after
TabLayoutMediator's current dispatch finishes. It also reconciles the highlighted
tab when scrolling settles, and cancels pending recovery/listeners on detach.
Never synchronously select another page inside a rejected page callback: the
mediator can recursively select the rejected page and overflow the stack.

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

## Downloading Graphy

The download action beside the guide offers PDF and PNG. `GraphyDocumentRenderer`
exports the immutable calculation snapshot, fully expanded, using the same
`GraphyCanvasRenderer` and layout primitives as the interactive screen. The fold
gutter, controls, focus and current scroll offset are excluded. Header expression
and final result are centered. Currency freshness/context notices are retained.

Exports use an isolated white-paper palette with canonical density/text scaling;
exporting never changes the selected app theme. Header, graph and footer are
measured separately. A footer rule and role swatches/labels form one row where
possible, never more than two; long translations widen the document rather than
colliding or shrinking text. Background app-name watermarks run diagonally at
low alpha. Their measured bounding regions cover at least 40% of the page; extreme
aspect ratios repeat the mark. Foreground node fills/text remain readable and
legend items do not overlap each other or the graph.

PNG is rendered at 2x where memory/dimensions permit, with a readable minimum 1x.
Oversized PNGs fail explicitly and offer PDF rather than clipping the diagram.
PDF is a single complete vector page. Encoding and file IO run off the UI thread.
A private prepared file freezes content before Android's Create Document picker;
its saved path survives picker recreation/rate updates. Save/cancel/failure cleans
it up; orphaned private exports expire after 24 hours. No storage permission or
manifest change is introduced. The created destination is removed after a failed
write where the provider supports deletion. Existing user files are not overwritten.

The PNG/document-region tests run on JVM; Android's native PDF backend is not
implemented there. Actual saved PDFs are pulled from the device and rendered for
visual verification. Storage reference verified 2026-10-21:
https://developer.android.com/training/data-storage/shared/documents-files

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
native renderer previews for nine examples × every app theme to
`app/build/reports/graphy/`. These are renderer previews, not device screenshots of
the complete workspace. Device navigation, large-font scrolling, TalkBack gestures
and Compose dialog behavior still require device/emulator QA.
See [audit and scope](GRAPHY_AUDIT.md) and [verification results](GRAPHY_VERIFICATION.md).
