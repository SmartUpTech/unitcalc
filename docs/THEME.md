# THEME SYSTEM
- Engine: ThemeApplier (View propagation). CalculatorTheme (model). CalculatorThemes (registry).
- Colors (Semantic): background (surface), mainText (content), functions (utility keys), operators (math), equal (accent/result).
- Logic: No hex in UI. Use background/transparent for keys. Font=@font/app_text. Text=@style/TextAppearance.Graphy.*.
- Propagation: Recursive ThemeApplier.apply(). Constraint: MaterialButtonToggleGroup buttons must keep background (don't set null). WindowChrome syncs status/nav bars.
- Ext: Add to CalculatorThemes for auto-support.
