# Finance calculators

Finance is the third drawer section: EMI, FD, RD, SIP. Existing EMI and catalog identities are retained. The new tools are separate destinations, with independent drafts, history and favorites, using the same Java/XML workspace, EMI adaptive layout, converter keypad, theme renderer, paged tables and Graphy primitives.

Catalog icons share a tintable 24dp vector style: EMI uses a payment statement/clock, FD a bank/lock, RD a calendar/repeat arrow and SIP a growth chart. `CalculatorCatalog` owns these mappings for the drawer, Explore and Favorites; do not duplicate tool icons in individual screens.

EMI/FD/RD/SIP share balanced result typography: 28sp primary values (22sp in compact layouts), 16sp result captions and 18sp supporting totals (16sp compact). Secondary guidance uses 16sp normally and the existing 13sp caption role only in the densest layout. `EmiLayout` reserves space above and below the result section: 16dp normally, 8dp compact and 4dp in the densest supported layout. Spare portrait space is shared above/below the summary; essential inputs and canonical keypad targets retain priority. These are shared resource roles, not per-tool styling.

## Authoritative calculations

`calculator/finance/investment/InvestmentCalculator` validates immutable `InvestmentInputs` and returns `InvestmentResult`/`InvestmentPeriod` snapshots. Every display, history entry, schedule and Graphy consumes these snapshots. Decimal inputs use DECIMAL128 intermediates and HALF_EVEN at currency boundaries. Posted period growth is the difference between rounded balances and contributions; totals therefore reconcile without accumulating monthly rounding error. Step-up contributions are rounded at each increase.

- SIP contributes at period start, then multiplies by `1 + annualPercentage/1200`. Optional step-up every 3/6/12 months adds an amount or multiplies the previous contribution by `1 + percentage/100`, starting with contribution 4/7/13. Disabled settings and independent amount/percentage drafts are retained. Return projections are not guaranteed.
- RD contributes at period start and uses the equivalent monthly factor `(1 + annualPercentage/400)^(1/3)` for quarterly compounding. A bounded decimal Newton iteration supplies the root, rather than binary floating-point powers.
- Cumulative FD compounds completed periods using the selected monthly/quarterly/half-yearly/yearly frequency, then accrues proportional simple interest for a partial final period. Its monthly schedule shows accrued estimated value, including unposted period interest.
- Periodic FD accrues simple interest on unchanged principal. Rounded cumulative interest entitlement determines payouts, avoiding repeated rounding drift. The final payout includes principal; the closing deposit balance is zero. Maturity proceeds are the final payment, whereas total receipts include earlier payouts. Annual yield is explicitly a hypothetical reinvestment-equivalent yield in this mode.

Dates label equal-month periods; these calculators do not implement actual-day bank accrual, tax, penalties, missed deposits, fees or bank-specific rules. RD schedule dates are contribution dates; closing values include that period's growth. FD dates are period-end dates. Date arithmetic always anchors to the original start day, clamping each month independently.

## Limits and state

Amounts are positive and below 10^12 at the currency's minor-unit scale; step-up amounts may be zero. Rates have at most four decimal places: FD/RD 0–100%, SIP −99.99–100%, step-up 0–100%. Step-up amounts cannot be negative. Tenure is 1–600 whole months. Start years are 1900–2100. Later stepped-up contributions must remain below 10^12, and totals below 10^18. Input parsing is length-bounded; invalid input clears stale results and cancels history recording.

New drafts use Settings' preferred currency. Existing drafts retain their currency; no exchange conversion occurs. Each tool stores a version-1 raw draft in its own preferences key and versioned history payload. Incomplete input is distinct from validated inputs. Small UI state is restored through saved instance state. Background calculations carry view/input revisions, so obsolete results cannot publish after edits or view recreation. Financial inputs are not logged or sent as analytics parameters.

## Validation

Run `:app:testDebugUnitTest` with `*finance.*`, `*model.CalculatorCatalogTest` and EMI regressions, followed by `:app:assembleDebug :app:lintDebug`. `InvestmentLayoutTest` renders real fragments into `app/build/reports/investments/`. These are native previews, not complete-device evidence. Screenshot review and observed viewport limitations are recorded alongside the review index. Extreme text/display settings that physically cannot fit show the existing shared space-limit notice; that notice does not establish single-screen compliance.

Reference examples: SIP 500/month, 12%, 120 months → 116169.54; RD 200/month, 6%, 60 months → 14012.71; cumulative FD 10000, 6%, quarterly, 36 months → 11956.18. All are before tax in a two-decimal currency.

## Implementation review (2026-11-02)

Debug build and lint passed (zero lint errors); 31 targeted tests passed, including shared history reuse. Native previews cover all 10 registered themes, all font choices at 1.3×, device-sized workspaces, and a wide workspace. Device evidence was captured on an Android 10 Mi A2 with 360×480dp available after tabs. The shared form adds a three-column numeric row and shorter accessible labels when the normal two-column arrangement cannot fit; inputs, results and the canonical keypad stay together. Full labels remain in accessibility descriptions.

Review artifacts: `app/build/reports/investments/index.html` and `VALIDATION.md`. Actual device captures are in its `device/` subdirectory; native previews are separate. A 320×300 workspace at 2× font scaling remains unsupported and shows the shared space-limit message. TalkBack execution, RTL translation review and an exhaustive device-density matrix remain unverified.

## Theme, spacing and primary-key refinement

The shared theme renderer highlights one action per keypad in priority order: Equal, Tick/Done, AC, then Backspace. Tick/Done uses Equal's appearance; AC retains its normal function style when either is present. Converter keypads without Equal/Done retain AC as their primary action. Separate keypads resolve independently, including settings previews.

Finance selectors reuse outlined field styling and the existing converter segment component, with theme-aware disabled text, icons and selected surfaces. Shared form layout prefers wider gutters and label spacing, then reduces spacing only when the available viewport requires it. Results stay visually connected to inputs while the keypad remains anchored. The latest build, 29 targeted tests (finance, shared EMI layouts and Appearance), and lint pass with zero errors. Refreshed device screenshots cover SIP step-up off/amount/percent, cumulative and periodic FD, and RD.

Result rows now have an explicit 8dp gap, reduced to 4dp in the densest layout. Finance fields and selectors use 4dp vertical outline insets while retaining 48dp touch bounds and zero vertical text padding. Removing the finance keypad's unused vertical container padding recovers space for the results; individual keys still use the canonical size and appearance. The shared result-spacing API leaves EMI's existing spacing unchanged by default.
