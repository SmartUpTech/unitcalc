# Graphy stages and folding: review brief

## Business requirements

- One continuous graph across Calculator, Unit and Currency Graphy, without step cards.
- A dependency stage may contain multiple independent calculations. Stage numbers
  represent dependency stages, not an operation count or evaluator execution order.
- Each semantic input/intermediate/result appears once. Consecutive calculations
  share result/operand nodes. Equal numeric values from different calculations stay distinct.
- Left-gutter IDE-style controls fold a whole stage; retain its output connection
  points. No separate accordion panels. The gutter remains visible during horizontal panning.
- Dynamic boxes preserve Oswald typography; only final results are bold. Inputs
  have solid outlines; intermediates use a single dashed outline and quiet fill.
- Vertical and horizontal scrolling are expressly authorized for Graphy only.
  Calculator/converter input screens retain the existing single-screen contract.
- Keep app bars, tabs, navigation, node details, guide and currency freshness notices.
- Preserve evaluator/converter results, precision, formatting and original dependencies.

## Shared implementation

`GraphStagePlan` validates source topology through GraphyTopology, groups operations
by dependency depth, defers original operands until their first consumer and
constructs a visible folded graph. Folding hides operations and exclusive original
inputs; produced results remain unique. Paths through hidden operations contract
into dependencies between visible boundary nodes. It never evaluates display text.

`GraphLayoutEngine` owns stage rows, content-based box sizes, operand order, 48dp
interaction space and intrinsic graph dimensions. Each original node keeps its id.
`OrthogonalConnectorRouter` handles continuous border-to-border arrows and bypasses.
`FlowchartGraphView` owns shared visuals, pinned gutter controls, hit testing and
accessible expand/collapse actions. Node details resolve to the original source
rather than the contracted presentation graph.

`GraphyPanel` hosts both scroll axes and saves the folded-stage set per semantic
calculation identity. Equivalent output reconstruction preserves folding. A new
calculation resets exploration. No evaluator, expression parser or rate fetching
changes are part of this implementation.

## Validation

Regression coverage includes simple chains, original operand order, shared nodes,
unchanged values/edges, distinct equal-valued branches, contracted boundary edges,
accessible fold actions and all 16 fold combinations for the eight-operation example.
The expected stages for `((12+8)*(9-4)+(18/3-2))/(7-3) = 26` contain 4,2,1,1 calculations.
Native renderer previews cover nine examples across all ten themes. Compose tests
exercise both scroll axes at 320x400 with normal/2x text in light/dark and reset on
new calculation. Automated accessibility actions do not constitute manual TalkBack QA.

Device checks use Mi A2 / Android 10. Screenshots from the stage implementation
are stored in `screenshots/graphy-stages/`; native full-diagram renders are in
`app/build/reports/graphy/`. Earlier concept and sequential-layout captures are
historical references and do not define the current approved behavior.

## Review scope

Prior Oswald typography, empty Graphy navigation and Back/lint fixes are in the
base commit. The working tree includes preceding connector and compact-geometry
fixes. No external review model was invoked. This document records the current
requirements and check results for the user's independent review.

## Check results (2026-10-21)

- Full suite: 177 tests across 34 classes, zero failures/errors.
- After final compact-width, operand-order and native gesture refinements: 33 focused Graphy/router
  tests passed, including minimum-API fold actions and all fold combinations.
- APK assembly and lint passed, zero errors and 247 existing warnings.
- Device: shared intermediate identity, folding/expansion, tab continuity,
  four-calculation first stage and final26. A real-gesture regression also checks
  horizontal movement, the right-most operand and folding from the panned position;
  the native Canvas delegates dragging/flinging to both Compose scroll states while
  keeping the workspace pager from intercepting graph gestures.
- Device dark theme at 1.3x text: folding and vertical access to final16 passed.
  This Android10 device rejects a 2x system font setting; 2x is covered by Compose tests.
- Manual spoken TalkBack and exhaustive arbitrary-expression validation remain
  outside these checks. No external reviewer/model was invoked.

## Download checks (2026-10-21)

- PDF/PNG exports use the complete expanded graph, white paper and a centered expression/result header.
- All four legend roles sit in a separated footer, in one row where space permits and at most two rows otherwise.
- The faint diagonal app-name watermark covers at least 40% of the measured page background; extreme aspect ratios repeat the mark.
- Full suite: 185 tests, zero failures/errors. Debug assembly and lint passed with zero errors and 246 warnings.
- Mi A2 / Android 10: saved both formats through the system picker from a folded eight-operation graph in dark mode; cancellation cleaned the prepared cache file.
- Inspected the saved PNG and rendered native PDF: complete graph, matching geometry, white background, compact footer and no overlapping foreground regions. PDF is one vector page.
- Automated checks cover narrow two-row footers, watermark extent/alpha, all ten app themes at 2x font scale, long headings, currency content, region separation and cache saving/failure cleanup.
- Review artifacts: screenshots/graphy-export/complete.png and complete.pdf (ignored local files).
