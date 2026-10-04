# THEME SYSTEM
- Engine: ThemeApplier (View propagation). CalculatorTheme (model). CalculatorThemes (registry).
- Colors (Semantic): background (surface), mainText (content), functions (utility keys), operators (math), equal (accent/result).
- Logic: No hex in UI. Use background/transparent for keys. Font=@font/app_text. Text=@style/TextAppearance.Graphy.*.
- Propagation: Recursive ThemeApplier.apply(). Constraint: MaterialButtonToggleGroup buttons must keep background (don't set null). WindowChrome syncs status/nav bars.
- Ext: Add to CalculatorThemes for auto-support.

## Theme preferences and progression
- Settings hosts a Compose theme-card row in the existing XML screen, using GraphyTheme and CalculatorTheme tokens. Theme names, plurals, states and dimensions are Android resources.
- ThemeProgression is a pure domain policy. The first three catalog ranks are available by default; every three foreground usage days unlocks the next rank. Constants live in ThemeProgression. CalculatorTheme.unlockRank is permanent metadata: do not renumber/reuse ranks when reordering/removing cards; append new ranks for new themes. Removed ranks remain reserved, so removing a theme cannot change any remaining theme's earned threshold.
- Preferences stores selected_theme (reused), theme_usage_days, theme_last_usage_date (ISO local date), theme_progression_version and one theme_grandfathered_id. Availability is derived; there are no per-theme unlock flags. Updates use one SharedPreferences batch. Backup/restore includes these preferences through the existing backup policy.
- Version 1 migration grandfathers an existing valid selected theme without adding artificial usage days. Its access remains after switching away. Unknown/removed/wrong-type selections fall back to the default; missing/corrupt progression values have safe defaults. Future progression versions are preserved.
- ThemeUsageTracker registers app foreground sessions with ProcessLifecycleOwner, off the main thread. Navigation, recomposition and Activity recreation do not create sessions; services, alarms and widgets do not count usage. Returning to the app after local midnight can count the new day. Remaining continuously foreground across midnight does not add usage until the next session.
- Only a date later than the last counted local date increments usage, by one even after skipped days. The persisted high-water date prevents rollback/replay and survives process restart/reboot/restore. Moving the clock far forward pauses further credit until the local date exceeds that date; V1 intentionally has no anti-cheat/reset mechanism.
- Unlocks never change selection. A locked tap shows its own derived remaining usage days, while the row shows one shared current cycle and the next locked theme. All-unlocked state hides the countdown; usage continues to support future catalog additions.
- ThemeSelectorViewModel publishes one immutable snapshot. The Compose row performs no preference reads or progression calculations, uses stable IDs, lifecycle-aware collection and saved lazy-list scroll state. ThemeManager/Preferences also reject locked or unknown selections.
