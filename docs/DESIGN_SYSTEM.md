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
- **Converter layout**: Unit and Currency share inline rows with right-aligned numeric amounts before the unit/currency dropdowns on the right; From/To labels are currently hidden and the swap action stays in its existing position. Amounts and selectors use matching themed outlines with aligned row heights, 8dp horizontal content insets and a 4dp gap between source/destination rows. Keep the source and destination rows above the unit category strip and the currency exchange-rate heading/Refresh row. Source and destination bounds must match across modes. Keep at least 4dp between the destination fields and the secondary strip, including compact spacing. Category tabs use 8dp horizontal padding, a 56dp minimum width, 2dp between icon and label, and a font-aware 64dp normal header. Use 8dp external top and bottom margins; the final constrained-window fallback retains 2dp margins and 2dp between value rows, reclaiming redundant card padding before declaring a space limit. Dropdowns reserve 25% of the tool viewport width plus the 10dp reclaimed by moving the swap control toward the end edge, regardless of amounts, with autosizing selected text. Unit names stay on one line while reducing by 2sp to 8sp; only names that still do not fit at 8sp may use a second line at 8sp. Both amount fields use regular weight and the same maximum font size, but each independently fits only its own text. Reserve their maximum-font line height so shrinking, deleting or clearing text cannot move selectors, spacing, swap or keypad. Only an empty input shows a zero hint; the empty output stays blank. They share a font-aware metadata slot, and the same four-column borderless keypad. AC sits beside 9; the last row is 0, 00, decimal, minus. Keep the existing app font. Converter has a single fixed workspace; its Convert/Graphy tabs are hidden while Graphy code is retained. Unit/Currency use an equal-width 41dp visual pill switch with 16sp labels and 48dp touch targets, a complete rounded selected segment, a subtle shared track and contrast-aware text on the selected theme accent. Unit categories are icon-and-label tabs in a horizontal scrolling picker; source/destination units keep their searchable dialogs; dropdown outlines use the shared semantic theme. Currency symbols use the same secondary theme color as the refresh timestamp and sit in separate single-line labels beside numeric amounts, retaining ISO codes in selectors and numeric-only editor contents. Currency picker details include country, ISO code and symbol. Both conversion-rate labels use the same 14sp role and secondary theme color. Currency has Refresh and a small “Last refreshed at:” timestamp beside the rate, without Share. If compact spacing and minimum key rows cannot fit, `ConverterControlsLayout` places category icons beside labels while both converter modes retain equal secondary-strip/metadata slots and a full-width category strip instead of shrinking typography or introducing scrolling.
- **Converter pickers**: All four unit/currency selectors open `ConverterPickerDialogFragment`, a centered searchable dialog with category/direction title, selected-item checkmark and empty-search state. Search covers labels, symbols, ISO codes and countries. Currency rows use rounded-square flags. Header/search/name/code sizes are 20/16/16/12sp; the floating search label is 10sp. Preserve selection identity while filtering and restore query/scroll after recreation. Keep the entire dialog above the IME; only its result list scrolls.
- **Calculator display**: Keep the expression above the result, including compact layouts. Constrain the result's height within its existing autosizing range when needed; restore the larger display when the viewport grows.
- **Dynamic values**: Calculator expression/result and both unit/currency amount editors refit as text or width changes. Reduce by 2sp from the styled maximum, clamping the last step to 8sp; restore the largest fitting size after deletion. `AutoSizeEditText` handles editable amounts; calculator displays use platform autosizing. Preserve the full value and selection. Values too long to fit at 8sp remain an unresolved finite-width constraint.
- **Spacing**: Use `@dimen` resources. Follow standard margins/paddings.
- **Keypad Grid**: Maintain 3 or 4 column structures for consistency.
- **Keypad text**: Shared text keys use `buttonTextSize` (26sp); the existing larger side-operator/equals role uses `buttonOperatorTextSize` (32sp). Both roles include the requested 4sp increase. Preserve readable text and complete controls within the available viewport; do not add tool-specific size overrides.
- **Corners/Elevation**: 4dp corners (`@style/RoundedCorners`). Flat design (0dp elevation), except preference theme previews, which use the shared 3dp theme-card elevation and 8dp shadow clearance. Preview backgrounds retain their exact theme colors. Theme cards use the shared 1dp outline, strengthened to 2dp for the applied card; the locked ad affordance uses Citizen Calculator’s ad/play icon inside a circular outline.
- **Interactions**: Use `@color/pad_button_ripple_color` for key ripples.
- **Accessibility**: All `ImageButton` elements MUST have a `contentDescription`.

- **History**: Secondary scrollable page with a History title and Back navigation to the previous workspace; hide bottom navigation while viewing it. Use compact date sections, single-line tool/time metadata, explicit ellipsis for long previews and selectable full-value details. Row options provide copying and deletion with Undo. Clear all requires confirmation. No search/filter controls currently.

## COMPOSE INTEGRATION
- **Graphy toolbar**: Download and busy indicators resolve the shared Graphy semantic colors on recomposition. Preserve distinct enabled/disabled states in every theme.
- **Usage**: Restricted to the **Graphy** layer.
- **Styling**: Must match XML spacing and shapes.
- **Theme**: Use `GraphyViewTheme` for Compose components.

## CONSTRAINTS
- **Converter viewport validation (2026-10-24)**: Native measurements verify matching From/To and keypad bounds across registered themes at 1.0/1.3 font scale after moving secondary strips below the fields. Two existing limits remain unresolved: a 320×400 workspace at 1.3 font scale and the 1080×1185 workspace at 520dpi in `currencyFitsActualDeviceViewportAtLargerDisplayDensity`. Regression tests confirm the same limits with the previous strip-first order. Those configurations show the existing space-limit message and do not establish single-screen compliance. The layout suite has 18 passes and these two failures.
- **Hardcoding**: Strictly forbidden. Use `strings.xml` and `dimens.xml`.
- **Reuse**: Always check `pane_*.xml` for reusable layouts before creating new ones.

