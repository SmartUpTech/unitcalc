---
name: new-converter
description: Detailed step-by-step guide for adding new conversion categories or units to the application.
---

# WORKFLOW: ADDING A NEW CONVERTER

## 1. DEFINE DATA MODELS
- **Identify Category**: Check if a new physical category (e.g., "Pressure") is needed.
- **Constants**: Add conversion factors to the relevant `Model` or `Constants` file.
- **IDs**: Create a new unique string ID for the converter.

## 2. RESOURCES
- **Icon**: Add a vector drawable for the category icon.
- **Strings**: Add the localized title in `strings.xml`.
- **Layout (Optional)**: If the standard `pane_calculator_converter` isn't sufficient, create a new `pane_*.xml`.

## 3. REGISTRATION
Register the new entry in `CalculatorCatalog.java`:
```java
register(entry("pressure", R.string.title_pressure, R.drawable.ic_pressure, Destination.UNIT_CATEGORY, R.id.menu_pressure));
```

## 4. UI BINDING
- If using a new pane, update `ConverterFragment` to handle the binding logic.
- Ensure the new unit category is handled in the logic switch cases.

## 5. VERIFICATION
- Verify the icon appears in the "Explore" or "Common" section.
- Test conversions for accuracy against known values.
- Verify theme propagation on the new pane.
