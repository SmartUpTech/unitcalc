---
name: data-storage
description: Details of the application's persistence layers including SQLite and SharedPreferences. Use this when modifying database schemas or preference handling.
---

# DATA STORAGE

## SQLITE (DatabaseHelper)
- **Table**: `calculation_history`
- **Fields**: `id`, `expression`, `result`, `created_at`, `calculator_id`.
- **Constraint**: Never perform SQL operations on the Main Thread.
- **History reads/deletion**: History uses a shared serial background executor and 50-record keyset pages ordered by descending database ID. Individual deletion targets the ID; Undo reinserts the original ID, timestamp, calculator and values with conflict-ignore. No schema migration is needed.
- **History scope**: Calculator, unit and currency results save after 3 seconds of idle time on a visible, resumed tool. Invalid/empty input cancels the pending result; pausing/hiding cancels the countdown and returning starts a fresh delay. Equals saves immediately through the same recorder. Consecutive identical snapshots are suppressed, including after saved-state recreation. Writes use the shared serial history executor. Unit entries carry the category catalog ID and readable source/destination names; currency entries include ISO codes. Converter entries open their tool but do not restore structured selections or historical rates. No database migration is required.

## SHAREDPREFERENCES (Preferences)
- BMI uses `bmi_state_v1` for its latest height/weight text, unit choices, focused field, precise canonical measurements after unit changes and history deduplication marker. Reset clears measurements/results while retaining units and existing generic history. This is app-private storage under the existing Android backup policy; measurements are never analytics parameters. BMI history uses `bmi_calculator` and the existing idle recorder/database, without a schema migration. Opening a BMI history row opens the latest remembered BMI state; its original values remain in the row details.
- **Usage**: App settings, last used states, and cached currency data.
- **Keys**: `selected_theme`, `number_decimals`, `last_conversion`, `currency_data`.
- **Currency cache**: `CurrencyRateStore` stores Frankfurter v2 USD-based rates, provider, per-quote observation dates and `fetchedAt` in `currency_data`. The payload and `currency_last_update` are written in one preference batch only after validation.
- **Currency refresh**: Automatic fetches use a 12-hour interval; manual refresh bypasses it. Failed fetches preserve the last valid cache and timestamp. Legacy `data`/`rates` caches remain readable offline and migrate after a successful fetch. Unsupported currencies have no fabricated rate.
- **Optimization**: Batch multiple updates using `apply()` instead of `commit()`.

## DATA INTEGRITY
- **Migrations**: Versioned migrations MUST be handled in `onUpgrade` of the `DatabaseHelper`.
- **Catalog Correlation**: `calculator_id` in history must always map to a valid ID in the `CalculatorCatalog`.

