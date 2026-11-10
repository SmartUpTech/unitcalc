# EMI Calculator

Offline fixed-rate monthly reducing-balance loans, available through Finance (the third side-navigation category), Explore and Favorites. FD, RD and SIP will use this same Finance category when implemented.

## Authoritative calculation

`EmiCalculator` builds immutable `EmiResult`/`EmiInstallment` snapshots from `EmiInputs` and `PrepaymentRule`. All UI, Graphy, charts, comparison and reports consume those snapshots. Formula intermediates use DECIMAL128; posted interest and payments use HALF_EVEN at the currency minor-unit scale. Totals sum posted rows, and the final installment closes the balance. Zero interest uses principal/months. A rounded payment that cannot reduce principal is rejected explicitly.

Limits: positive principal below 10^12 with currency precision, annual percentage 0–100 with at most four fractional digits, 1–600 whole months, up to 20 one-time/monthly/yearly prepayment rules, and first-payment years 1900–2100. Years must represent exact whole months. Dates are anchored to the original first-payment day, clamped individually at month ends. Dates do not introduce daily accrual.

Prepayments post after the selected EMI, aggregate coincident rules, and cap at the remaining balance. Excess in the payoff installment is disclosed; future rules are not applied after payoff. Reduce tenure retains the original regular payment. Reduce EMI recalculates the following payment over the original remaining term. Savings compare the same loan without prepayments.

Reference posting fixture: 1,000,000 at 8.5% over 240 months: regular EMI 8,678.23; interest 1,082,776.63; total 2,082,776.63. Principal plus interest equals scheduled payment; scheduled principal plus prepayments equals the original principal.

## UI and reuse

Calculate/Schedule/Graphy use the shared workspace pager and tab controller. Native Calculate keeps inputs visible while the shared converter keypad replaces the result card during editing. Done/Back collapses the keypad and reveals a valid result. Filled Details and text Share match Age actions; Details retains prepayments, comparison, report/export, Reset and Insights. `EmiLayout` compacts spacing and reflows wide windows; `CalculationViewport` exposes physically impossible configurations using the shared `ViewportContent` measurement contract. No scrolling fallback exists. Schedule and prepayments use `FixedTableView` measured pagination. Secondary views retain bottom navigation. Charts have textual equivalents and native keyboard/TalkBack selection.

Themes, bundled fonts, button styles/shapes and feedback use the existing owners. Amount labels use currency symbols only. Chart data is content-only; `ValueChartRenderer` owns shared drawing for UI/PDF. Graphy uses existing input/operation/derived/result primitives, including the zero-interest rule and prepayment-interest comparison.

## Persistence and exports

`preferred_currency` is shared through Preferences, initialized once from supported device-region currency with INR fallback. The existing `CurrencyCatalog` owns converter metadata and stable index order. Converter selections remain independent. Existing EMI drafts retain their currency; reset creates a new loan using the current preference. No exchange conversion occurs.

`emi_workspace_v1` holds compact raw drafts and presentation choices. Small transient UI state and selection survive saved-state restoration. Versioned history payloads are stored in the nullable schema-v4 `payload` column; old rows remain readable and Undo retains their identity/payload. The shared three-second idle recorder handles deduplication and invalidation. Financial values are never analytics parameters or diagnostic log content.

PDF/CSV reports use immutable snapshots and background work. PDF uses A4 pagination, repeated table headers, full scheduled amounts, selected font and white-paper palette. CSV uses UTF-8, quoted fields, canonical decimals and ISO dates. `PreparedDocumentFiles` shares the Graphy save/cancel/error lifecycle. Save uses Create Document; sharing exposes only `cache/emi_exports/` through a non-exported FileProvider with temporary read grants. Shared files remain readable for recipients and expire after 24 hours on later export cleanup.

## Validation

Run `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Focused tests are `EmiCalculatorTest`, `EmiIntegrationTest`, `EmiFragmentTest`, and native-rendered `EmiLayoutTest`, plus shared history/export regressions. Native previews are generated under `app/build/reports/emi/`; previews alone are not full-device verification. Actual saved PDFs must be pulled and visually rendered on the connected device before release certification. Record remaining viewport, accessibility and device limitations with release evidence.

Official references checked 2026-11-02:
- https://www.hdfc.bank.in/personal-loan/emi-calculator
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/reference/androidx/core/content/FileProvider
- https://support.google.com/googleplay/android-developer/answer/10144311

Actual lender schedules can differ. Fees/APR, taxes, daily-interest adjustments, floating/flat rates, moratoriums and balloon payments are excluded. Publishing and Play Console declarations are separate release tasks.

## Recorded review evidence — 2026-11-02

EMI checkpoint `866a44a`, with native graphics enabled for `EmiFragmentTest`, has 37 passing focused checks across calculation, state, UI, catalog/navigation, history and document lifecycle coverage. The native graphics setting is required for the bundled fonts to have real measurements; legacy font measurements do not establish viewport compliance. An isolated checkout was used for the final fragment checks while other finance tools were being developed in the primary checkout.

Debug assembly succeeded. The latest project lint run completed with zero errors and 506 warnings. The full suite ran 380 tests: seven failed and seven were skipped. The EMI measurement test and the obsolete four-category expectation were subsequently corrected and verified in the focused checks. Five broader UI failures remain: Graphy panel output refresh, Age search-cursor contrast, BMI equals-icon color, and two converter viewport checks. Production release remains uncleared.

On-device checks used a separate review installation, preserving the existing app and its data. Recorded screens cover the third Finance category, calculation, editing, incomplete/rounding states, schedule, Insights/balance, history restoration and Graphy. The Graphy heading was removed; the persistent notice is one line, with complete assumptions retained in help. Secondary pages hide the underlying workspace from accessibility.

The saved reference report was pulled through the document-picker workflow and rendered for review: 16 A4 pages, portrait summary and landscape tables. Its extracted 240 schedule rows reconcile principal, interest and total payments to the reference fixture, with a zero closing balance. Currency symbols and the selected Oswald font render correctly. The broader device/font/currency PDF matrix and extreme accessibility configurations still require release validation; the tested 320×300 viewport at 2× text scale is explicitly unsupported rather than clipped.

Review artifacts are stored outside the build directory under the calling chat's `emi-review` visualization folder, including screenshots, the native PDF and focused/full-suite XML evidence.
