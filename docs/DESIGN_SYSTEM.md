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
- **Converter layout**: Unit and Currency share inline rows with right-aligned numeric amounts before the unit/currency dropdowns on the right; From/To labels are currently hidden and the swap action stays in its existing position. Dropdowns reserve 25% of the tool viewport width regardless of amounts, with autosizing selected text. Unit names stay on one line while reducing by 2sp to 8sp; only names that still do not fit at 8sp may use a second line at 8sp. Both amount fields use regular weight and the same maximum font size, but each independently fits only its own text. Reserve their maximum-font line height so shrinking, deleting or clearing text cannot move selectors, spacing, swap or keypad. Only an empty input shows a zero hint; the empty output stays blank. They share a font-aware metadata slot, and the same four-column borderless keypad. AC sits beside 9; the last row is 0, 00, decimal, minus. Keep the existing app font. Converter has a single fixed workspace; its Convert/Graphy tabs are hidden while Graphy code is retained. Unit/Currency use an equal-width, joined segmented control with 48dp touch targets. Unit categories are icon-and-label tabs in a horizontal scrolling picker; source/destination units keep their searchable dialogs; dropdown outlines use the shared semantic theme. Currency has Refresh and a small “Last refreshed at:” timestamp beside the rate, without Share. If compact spacing and minimum key rows cannot fit, `ConverterControlsLayout` places metadata beside the refresh header (the category strip always keeps full width) instead of shrinking typography or introducing scrolling.
- **Converter pickers**: All four unit/currency selectors open `ConverterPickerDialogFragment`, a centered searchable dialog with category/direction title, selected-item checkmark and empty-search state. Search covers labels, symbols, ISO codes and countries. Currency rows use rounded-square flags. Header/search/name/code sizes are 20/16/16/12sp; the floating search label is 10sp. Preserve selection identity while filtering and restore query/scroll after recreation. Keep the entire dialog above the IME; only its result list scrolls.
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

