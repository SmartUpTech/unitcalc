---
name: architecture
description: Guidelines for the application's module structure, package organization, and core architectural principles. Use this when adding new features or refactoring existing modules.
---

# ARCHITECTURE

## STRUCTURE
- **Module**: `:app` (Monolithic Android Application).
- **Packages**:
    - `activity`: Entry points (MainActivity, SettingsActivity).
    - `calculator/basic`: Basic calculator UI.
    - `calculator/finance/emi`: EMI UI, calculations, schedules and reports.
    - `calculator/finance/investment`: Shared FD, RD and SIP UI, calculations and Graphy content.
    - `calculator/tools/{age,bmi,fuel,percentage,timer}`: Tool-specific UI, state and logic. Timer's Compose/model/service/notification/alarm/widget subpackages stay here.
    - `converter`: Shared converter workspace and picker; `converter/unit` and `converter/currency` own their respective screens.
    - `fragment`: Shared Explore, Favorites and History screens, not calculator/converter implementations.
    - `model`: Data structures and domain entities.
    - `theme`: Visual styling and theme propagation.
    - `graphy`: Calculation visualization engine.
    - `helper`: Utilities and logic helpers.
    - `utils`: Shared expression evaluation, decimal arithmetic and conversions.
    - `widget`: Shared responsive viewport, numeric editor and keypad layouts.
    - `games`: Existing games host and state.

All package paths above are relative to `net.smartlogic.unitconverter`. Navigation groups are registered in `model/CalculatorCatalog.java`; `activity/MainActivity.java` dispatches destinations. FD/RD/SIP reuse one investment implementation rather than separate tool copies. XML resource filenames and persisted catalog IDs/preferences need not match Java package paths.

`WorkspacePagerAdapter` normally inflates plain view panes. A pane containing an XML `FragmentContainerView` must receive its owning fragment's `getLayoutInflater()` via the adapter overload. This preserves the child FragmentManager factory; an activity-context inflater attaches the keypad to the activity and breaks parent-fragment key callbacks. Fuel and Percentage use the fragment-owned overload.

## CORE RULES
- **Logic Placement**: Keep logic in Helpers/Models. Fragments should focus on UI binding and Lifecycle.
- **Data Flow**: UI -> Model/Helper. Minimize state within Fragments.
- **Navigation**: Managed via `ViewPager2` and `TabLayout`. Reference `CalculatorCatalog` for destinations.
- **Threading**: Use existing bounded executors or coroutines for background tasks. `View.post` schedules work on the UI thread; it is not a background executor. Never perform SQL or network calls on the Main Thread.
- **Persistence**: Use the `Preferences` helper for settings and `DatabaseHelper` for history.
- **Files**: One class per file. Avoid premature abstraction. Maintain strict naming conventions.

