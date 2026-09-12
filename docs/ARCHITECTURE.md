# ARCHITECTURE
- Module: :app (monolith)
- Pkg: activity (entry), fragment (UI), model (data), theme (visual), graphy (engine), helper (utils).
- Files: MainActivity (root), CalculatorCatalog (registry), ThemeApplier (propagation), DatabaseHelper (SQL), Preferences (prefs).
- Rules: Fragment=UI/Lifecycle. Logic in Helper/Model. UI -> Model/Helper. Min state in Fragment. Nav: ViewPager2/TabLayout. Reuse helper/utils. MainThread: View.post/Coroutines.
- Persistence: Preferences helper (semantic keys).
- Naming: 1 class/file. Avoid premature abstraction.
