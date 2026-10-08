---
name: design-system
description: Guidelines for UI consistency, component usage, and layout rules. Use this when creating new UI screens or modifying existing layouts to ensure they adhere to the Calculator+ design system.
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
- **Single-screen tools**: Every calculator, converter and utility must keep its input, output, selectors, essential actions and keypad within the available viewport. No scrolling screen, embedded scrolling content panel or scrolling overflow fallback. Adapt after chrome/insets; never clip or conceal inaccessible controls. Report any physically impossible accessibility case as unresolved.
- **Keypad parity**: Reuse canonical components/styles and shared responsive tokens. Preserve consistent typography, key roles/colors, shape, spacing, icons, touch targets and feedback across tools and themes. Different key sets are allowed; independent keypad visual styles are not.
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

