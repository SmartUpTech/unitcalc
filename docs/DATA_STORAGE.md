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
- **History scope**: The basic calculator currently writes history. Legacy entries from other tools can open their tool, but do not contain enough structured input to restore units or rates.

## SHAREDPREFERENCES (Preferences)
- **Usage**: App settings, last used states, and cached currency data.
- **Keys**: `selected_theme`, `number_decimals`, `last_conversion`, `currency_data`.
- **Currency cache**: `CurrencyRateStore` stores Frankfurter v2 USD-based rates, provider, per-quote observation dates and `fetchedAt` in `currency_data`. The payload and `currency_last_update` are written in one preference batch only after validation.
- **Currency refresh**: Automatic fetches use a 12-hour interval; manual refresh bypasses it. Failed fetches preserve the last valid cache and timestamp. Legacy `data`/`rates` caches remain readable offline and migrate after a successful fetch. Unsupported currencies have no fabricated rate.
- **Optimization**: Batch multiple updates using `apply()` instead of `commit()`.

## DATA INTEGRITY
- **Migrations**: Versioned migrations MUST be handled in `onUpgrade` of the `DatabaseHelper`.
- **Catalog Correlation**: `calculator_id` in history must always map to a valid ID in the `CalculatorCatalog`.

