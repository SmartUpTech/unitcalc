# THEME SYSTEM
- Engine: ThemeApplier (View propagation). CalculatorTheme (model). CalculatorThemes (registry).
- Colors (Semantic): background (surface), mainText (content), functions (utility keys), operators (math), equal (accent/result).
- Logic: No hex in UI. Use background/transparent for keys. Font=@font/app_text. Text=@style/TextAppearance.Graphy.*.
- Propagation: Recursive ThemeApplier.apply(). Constraint: MaterialButtonToggleGroup buttons must keep background (don't set null). WindowChrome syncs status/nav bars.
- Ext: Add to CalculatorThemes for auto-support.

## Shared foreground and selection colors

`CalculatorTheme` owns `primaryContentColor`, `secondaryContentColor`,
`operatorContentColor`, `accentContentColor`, `onAccentColor` and the shared
`readableColor(preferred, surface)` resolver. Foreground roles require normal-text
contrast (4.5:1), including operators used in smaller expression text. XML,
Compose, Graphy, dialogs, catalog rows, games and window chrome consume the same
roles. Selected tabs, category icons, bottom navigation and equals use accent
ink; filled converter segments pair that accent with on-accent content.

All catalog palettes supply readable primary, secondary and accent ink directly.
Glacier, Sage Green and Golden Poppy use dark accents on their lighter surfaces;
Purple Mountain Majesty uses deep-purple ink. Burnt Orange uses the deeper
approved background to retain warm light text. Theme IDs, unlock ranks and
catalog order remain permanent. System-bar icon mode chooses the higher-contrast
black/white foreground, rather than using a fixed brightness threshold.

Navigation drawer subheaders receive the shared secondary role explicitly.
Converter outlines use that role at full opacity; picker text/checks resolve
against the selected-row surface. Compose container and inverse roles are
mapped to the app palette instead of inheriting unrelated Material defaults.
Theme-card labels reserve the maximum measured wrapped-name height equally
across the catalog, while retaining the shared card elevation and exact preview
backgrounds.

## Shared typography

Use regular weight by default. Bold requires an explicit user request; it must not be introduced as automatic field or heading emphasis.

Oswald is the app font. XML Views and Canvas resolve `@font/app_text`, which
contains the bundled regular (400) and bold (700) weights. Compose uses
`theme/AppTypography.kt` (`AppFontFamily` and `AppTypography`) with those same
bundled files. `GraphyTheme` supplies this complete Material type scale to every
Compose host, including Settings and Timer. Define every text role, not only
body text: app bars use `titleLarge` and dialogs use headline styles. The XML
theme similarly maps all Material text appearances and sets both framework and
AppCompat font-family attributes. Keep font sizes/weights semantic; do not fix
individual screens with font overrides or recursive runtime typeface changes.

## Theme preferences and access
- Porcelain White (`porcelain_white`), Oyster Cream (`oyster_cream`) and Titanium Gray (`titanium_gray`) are permanently free. Porcelain White remains the default; names and IDs do not change. Other themes require Pro or an independent rewarded expiry later than the current epoch-millisecond time. Temporary ad-free access is unrelated.
- `ThemeAccess` owns the access policy and seven continuous 24-hour duration. Debug and release use the same locks; debug ads use Google's test unit. Stable catalog order and IDs are preserved; old unlock ranks remain metadata, not entitlement.
- `Preferences` stores `selected_theme`, `theme_expiry_<id>`, `pending_rewarded_theme`, `theme_access_version`, `theme_access_notice` and `theme_reward_attempt_<id>`. Rewards persist expiry, attempt ID and pending theme in one background commit. Duplicate attempts and early renewals cannot extend expiry. Selection and clearing pending/notice use one preference batch.
- Migration preserves free selections silently and Pro selections. Other non-Pro selections fall back to Porcelain White with a one-time explanation and no grace period. Usage-day and grandfathered keys are retired. Unknown, removed and corrupt selected IDs are repaired to Porcelain White.
- `ThemeManager.setProAccessProvider` accepts the paid entitlement source before initialization; default is non-Pro. Call `refreshAccess` when that source changes. Billing integration is not included. Never supply temporary ad-free state.
- `ThemeRewardCoordinator` owns the guarded reward transaction across Activity loss. Only the SDK earned callback grants access. Apply after ad closure and durable persistence; startup/foreground finishes valid pending rewards. Loading, failure, cancellation and unrewarded dismissal grant nothing. Themes use a separate callback route from games through the consent-gated Next-Gen manager; all fullscreen paths exclude each other.
- `ThemeAccessLifecycle` validates on startup and app foreground/Activity resume; ThemeManager validates before selection. No timer changes appearance in uninterrupted use. Expired unused themes only become locked; expired selection falls back to Porcelain White and queues one notice. A valid pending reward is recovered first. Explicit available selection clears stale notices, including selection of the already-visible theme.
- Preferences keeps the existing Compose horizontal row and stable order. Every card retains its exact palette, elevated same-background swatches, free/Pro/ad/remaining-time status and accessible radio semantics. Locks show a top-right lock and Citizen Calculator ad/play icon inside a circular outline; available cards show an outlined trophy; applied cards add a check and stronger outline. Opening Preferences brings the applied card into view. Names/statuses reserve measured font-scaled heights equally.
- Local epoch time is intentionally device-clock dependent; time-zone changes do not change the duration. Crash recovery covers completed durable reward writes; toast display cannot be strictly exactly-once across process termination.
- Tests cover migration, independent expiry, duplicate grants, Pro changes, pending recovery, ad callback ordering and native-rendered picker bounds. Runtime/device evidence and remaining limitations are recorded with the implementation review.

Debug builds make all known themes permanently available without reward ads or expiry, and label cards “Available in debug”. This does not grant Pro or write reward expiries. Release builds retain the free/Pro/seven-day reward access rules.
