---
name: data-storage
description: Details of the application's persistence layers including SQLite and SharedPreferences. Use this when modifying database schemas or preference handling.
---

# DATA STORAGE

## SQLITE (DatabaseHelper)
- **Table**: `calculation_history`
- **Fields**: `id`, `expression`, `result`, `created_at`, `calculator_id`.
- **Constraint**: Never perform SQL operations on the Main Thread.

## SHAREDPREFERENCES (Preferences)
- **Usage**: App settings, last used states, and cached currency data.
- **Keys**: `selected_theme`, `number_decimals`, `last_conversion`, `currency_data`.
- **Optimization**: Batch multiple updates using `apply()` instead of `commit()`.

## DATA INTEGRITY
- **Migrations**: Versioned migrations MUST be handled in `onUpgrade` of the `DatabaseHelper`.
- **Catalog Correlation**: `calculator_id` in history must always map to a valid ID in the `CalculatorCatalog`.

