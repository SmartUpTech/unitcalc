---
name: design-system
description: Guidelines for UI consistency, component usage, and layout rules. Use this when creating new UI screens or modifying existing layouts to ensure they adhere to the Graphy Calculator design system.
---

# DESIGN SYSTEM

## COMPONENTS
- **Keypad**: Use `pane_calculator_calculate` for standard layouts.
- **Result Display**: Apply `@style/ResultDisplay`.
- **Cards**: Wrap sections in `@drawable/bg_converter_card`.
- **Lists**: Use `HorizontalListView` for unit categories.
- **Tabs**: Styled with `@style/WorkspaceTabLayout`.
- **Buttons**: Themed `MaterialButton`. Calculator keys must use `@style/CalculatorKey`.

## LAYOUT RULES
- **Spacing**: Use `@dimen` resources. Follow standard margins/paddings.
- **Keypad Grid**: Maintain 3 or 4 column structures for consistency.
- **Corners/Elevation**: 4dp corners (`@style/RoundedCorners`). Flat design (0dp elevation).
- **Interactions**: Use `@color/pad_button_ripple_color` for key ripples.
- **Accessibility**: All `ImageButton` elements MUST have a `contentDescription`.

## COMPOSE INTEGRATION
- **Usage**: Restricted to the **Graphy** layer.
- **Styling**: Must match XML spacing and shapes.
- **Theme**: Use `GraphyViewTheme` for Compose components.

## CONSTRAINTS
- **Hardcoding**: Strictly forbidden. Use `strings.xml` and `dimens.xml`.
- **Reuse**: Always check `pane_*.xml` for reusable layouts before creating new ones.

