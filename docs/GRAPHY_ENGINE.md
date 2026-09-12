# GRAPHY
- Logic: Viz layer. Non-modifying. Flow: Calc -> GraphyOutput -> Compose Render.
- Model: GraphyNode (val/op), GraphyConnection (flow), GraphyNodeType (type).
- Engine: GraphLayoutEngine (coords), OrthogonalConnectorRouter (paths).
- Principles: Explains intermediate steps. Subtle animation. Min Compose recomposition. Generic components only.
- States: Clear empty/error states if expression not supported.
