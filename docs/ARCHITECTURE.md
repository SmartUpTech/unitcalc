---
name: architecture
description: Guidelines for the application's module structure, package organization, and core architectural principles. Use this when adding new features or refactoring existing modules.
---

# ARCHITECTURE

## STRUCTURE
- **Module**: `:app` (Monolithic Android Application).
- **Packages**:
    - `activity`: Entry points (MainActivity, SettingsActivity).
    - `fragment`: UI controllers and lifecycle management.
    - `model`: Data structures and domain entities.
    - `theme`: Visual styling and theme propagation.
    - `graphy`: Calculation visualization engine.
    - `helper`: Utilities and logic helpers.

## CORE RULES
- **Logic Placement**: Keep logic in Helpers/Models. Fragments should focus on UI binding and Lifecycle.
- **Data Flow**: UI -> Model/Helper. Minimize state within Fragments.
- **Navigation**: Managed via `ViewPager2` and `TabLayout`. Reference `CalculatorCatalog` for destinations.
- **Threading**: Use `View.post` or Coroutines for background tasks. Never perform SQL or network calls on the Main Thread.
- **Persistence**: Use the `Preferences` helper for settings and `DatabaseHelper` for history.
- **Files**: One class per file. Avoid premature abstraction. Maintain strict naming conventions.

