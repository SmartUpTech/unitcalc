# Fuel cost calculator

Trip fuel volume and cost from distance, fuel consumption, price per litre, passengers, and optional round trip. Display name: **Fuel cost calculator**. Registered under **Drawer → Tools** with the unchanged stable ID `fuel_efficiency_calculator`. Distinct from the **Fuel** unit converter category (`unit:6`), which only converts consumption units. Existing source/resource names, saved drafts and analytics IDs remain compatible. Fuel needed uses the shared 16sp primary supporting-result role rather than the 12sp input-label role.

## Calculation

- Distance converts through `Conversions.convert` to kilometres.
- Effective distance doubles when **Round trip** is on.
- Efficiency normalizes to L/100 km via `Conversions.convertFuelValue` (existing zero-fuel rules unchanged).
- Fuel litres = effective km × (L/100 km) ÷ 100.
- Trip cost = fuel litres × price per litre (HALF_EVEN at currency minor units).
- Cost per passenger = trip cost ÷ passengers (1–99).

Golden fixture: 420 km, 15 L/100 km, ₹102.50/L, round trip, 4 passengers → 126.0 L, ₹12,915.00, ₹3,228.75 each.

## UI

- Shared workspace shell: **Calculate | Graphy** tabs (`fragment_calculator`).
- Calculate pane: converter-style amount fields and unit selectors (`converter_amount` / `converter_selector`), `AutoSizeEditText` refit (including compact layout), 12-character amount cap (2 for passengers), preferred-currency symbol on price per litre, collapsible `converter_keypad`, result card.
- Graphy: flowchart via `FuelEfficiencyGraphBuilder` (same numbers as Calculate).
- App bar: History + Reset (shared reset menu with BMI).
- Bottom navigation stays visible; no new bottom-nav destination.

## Persistence

- Draft: `fuel_efficiency_state_v1` in app preferences (JSON).
- History: shared 3 s idle recorder; `calculator_id` = `fuel_efficiency_calculator`. Payload JSON restores all inputs on reuse; legacy rows without payload fall back to parsing the visible expression.

## Out of scope (v1)

Speed, time-at-speed, multi-leg trips, price per gallon (price is per litre with `{symbol}/L` label), favorites.
