# Graphy standardization audit (release/v11)

## Existing implementation
- `GraphyOutput`, semantic `GraphyNodeType`, connections and evaluation traces are reusable.
- Calculator and unit/currency converter workspaces already share `GraphyPanel` →
  `FlowchartGraphView`; converter Graphy does not bypass the renderer.
- `GraphLayoutEngine` has expression/operator-specific and converter-branch layouts;
  `OrthogonalConnectorRouter` is reusable.
- XML tokens, `GraphyViewTheme` and Compose tokens overlap. Compose colors retain a
  separate fixed palette; Compose's result card does not match the graph result.
- Canvas nodes have hardcoded shadows, single-line values, no node exploration or
  virtual accessibility nodes. Overflow is handled by shrinking the whole graph.
- `AndroidView.update` recreates the graph View and layout on every update.
- Converter builders infer operations from strings. Temperature shows only scale
  names; reciprocal fuel transformations are incorrectly described as multiplication.
- Currency uses the constant primitive, ignores cached-rate timestamps, and hides zero.
- Expression result nodes format doubles separately from the calculator result.
- Existing tests cover expression graphs, basic converter branches and connector routing.

## Scope and plan
1. Keep the existing semantic output, calculator evaluation trace, renderer and router.
2. Unify theme roles through one shared token resolver; use flat semantic cards,
   visible role labels, compact operations and one emphasized result node.
3. Make layout depend on dependency structure, wrap long values, preserve font scale,
   and expose nodes/details in dependency order to touch and TalkBack.
4. Add immutable linear/affine/rate/reciprocal/composite/direct conversion metadata.
   Adapt existing conversion metadata/functions without changing their mathematics.
   Gas Mark is piecewise; fuel retains legacy rounding. Do not claim these are linear.
5. Migrate all unit categories and currency to the same builder; consume existing
   formatted results and cached-rate time. Remove obsolete string inference/branches.
6. Add transformation, structure, formatting, theme, layout and interaction tests;
   run build/lint and capture representative renderings if runtime support is available.

## Verification matrix
| Existing screen | Coverage |
| --- | --- |
| Basic calculator: percentage, multi-input, nested expression, sqrt | trace/format/merge |
| Length, mass, area, volume, speed, time, data, cooking, power, pressure, energy, torque | linear/reverse |
| Temperature, reverse temperature, Gas Mark | affine/piecewise |
| Fuel consumption | reciprocal/legacy rounding |
| Currency | dynamic rate/zero/missing rate/cached time |
| Every catalog theme; narrow width and large font | semantics/contrast/layout |

EMI, GST, SIP, Compound Interest, BMI, geometry and age calculators do not exist in
v11's catalog. No charts exist to consolidate. Adding those products is outside this
visual refactor; future implementations must use the shared Graphy contract.
