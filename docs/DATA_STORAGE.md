# STORAGE
- SQL (DatabaseHelper): calculation_history (id, expression, result, created_at, calculator_id).
- Prefs (Preferences): selected_theme, number_decimals, last_conversion, currency_data.
- Rules: Batch Pref updates (apply). No SQL on MainThread. Versioned migrations in onUpgrade. Correlate calculator_id with Catalog.
