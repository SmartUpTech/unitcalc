# Percentage Calculator

Dedicated four-mode percentage tool under **Drawer → Calculators** as `percentage_calculator`. Distinct from the basic calculator `%` operator.

## Modes

| Mode | Formula |
|------|---------|
| Percent of | result = base × percent ÷ 100 |
| Increase | amount = base × percent ÷ 100; result = base + amount |
| Decrease | amount = base × percent ÷ 100; result = base − amount |
| What percent | result = part × 100 ÷ whole |

Golden fixture: Increase 250 by 12% → 280 (amount 30).

Arithmetic uses `BigDecimal` via `DecimalMath.divide` / checked multiply-add. Percent must be ≥ 0. Whole must be ≠ 0 for What %. Display rounds HALF_EVEN at the UI boundary using Settings decimals.

## UI

- Shared workspace shell: **Calculate | Graphy** tabs (`fragment_calculator`).
- Calculate pane: mode toggle, converter-style `AutoSizeEditText` amounts (`converter_amount`), `%` suffix for percent modes, collapsible `converter_keypad`, result card.
- Graphy: flowchart via `PercentageGraphBuilder` (same numbers as Calculate).
- App bar: History + Reset. Bottom navigation stays visible; no new bottom-nav destination.

## Persistence

- Draft: `percentage_state_v1` in app preferences (versioned JSON).
- History: shared 3 s idle recorder; `calculator_id` = `percentage_calculator`. Payload restores mode and both fields; legacy expression fallback parses `Mode · a · b%` lines.

## Out of scope (v1)

GST, Discount, currency-aware pricing, favorites eligibility, replacing basic calculator `%`.
