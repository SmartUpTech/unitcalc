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
- **Converter layout**: Unit and Currency share inline rows with left-aligned amounts before the unit/currency dropdowns on the right; From/To labels are currently hidden and the swap action stays in its existing position. They share a font-aware metadata slot, and the same four-column borderless keypad. AC sits beside 9; the last row is 0, 00, decimal, minus. Keep the existing app font and workspace tabs. Mode selectors are independent rounded filter chips with 48dp touch targets; dropdown outlines use the shared semantic theme. Currency has Refresh and a small “Last refreshed at:” timestamp beside the rate, without Share. If compact spacing and minimum key rows cannot fit, `ConverterControlsLayout` places metadata beside the category/refresh header instead of shrinking typography or introducing scrolling.
- **Calculator display**: Keep the expression above the result, including compact layouts. Constrain the result's height within its existing autosizing range when needed; restore the larger display when the viewport grows.
- **Dynamic values**: Calculator expression/result and both unit/currency amount editors refit as text or width changes. Reduce by 2sp from the styled maximum, clamping the last step to 8sp; restore the largest fitting size after deletion. `AutoSizeEditText` handles editable amounts; calculator displays use platform autosizing. Preserve the full value and selection. Values too long to fit at 8sp remain an unresolved finite-width constraint.
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

