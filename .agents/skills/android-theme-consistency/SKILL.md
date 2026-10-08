---
name: android-theme-consistency
description: Implement consistent Android UI themes, resources and shared components in XML Views and Compose. Use for screen styling, light/dark mode, theme selection, typography, keypad/expression colors, accessibility or removing hardcoded values.
---

# Android Theme and Consistency

## Calculator+ keypad contract

Enforce AGENTS.md's mandatory Tool Screen UX rules. Reuse shared keypad components and canonical key styles across calculators, converters and utilities. Keep equivalent number/function/operator/result keys consistent in font, semantic color, background, shape, spacing, icon treatment, touch target and pressed/haptic/sound feedback. Allow tool-specific key sets, not tool-specific styling. Derive responsive sizing from shared tokens and the available viewport; preserve the selected theme immediately. Compare actual rendered keypads at matching viewport and theme settings, including large text, before claiming parity. Do not turn the screen or keypad into scrolling content to make it fit.

1. Read docs/THEME.md and docs/DESIGN_SYSTEM.md, then trace the affected component to its current token owner. Reuse CalculatorTheme/CalculatorThemes, ThemeApplier, WindowChrome and the existing Compose theme bridge where present. Confirm names in source before editing. Preserve flat, borderless styling and established hierarchy; do not create screen-specific palettes or competing theme managers.
2. Keep user-facing text, plurals, accessibility labels and formatting templates in localized resources. Keep reusable spacing, sizes, typography and shapes in existing resources/tokens. Keep colors in the central semantic theme model. Keep preference keys, navigation IDs and conversion definitions in their appropriate shared owners. Avoid magic values in UI/logic and duplicated constants. Values must be defined somewhere: named, documented canonical resource/model definitions are allowed; do not move calculation factors into UI resources or turn trivial language literals into needless abstractions. Keep these skill rules free of app-specific palettes or dimensions.
3. Map the same semantic role consistently across keypad, expression, results, history, dialogs and Graphy. Preserve the app's number/operator/function/equal role contract. Use matching foreground/background pairs for normal, disabled, pressed, selected, error and focused states. Reuse components and typography instead of tuning each screen separately.
4. Support light and dark themes for every changed surface, including dialogs, sheets, popup menus, icons, charts and system bars. Respect the existing theme preference and system-follow behavior where available; if a mode is missing, report and implement the required support within scope rather than silently treating a fixed palette as dark mode. Apply theme changes immediately through the shared state and persist using existing preferences. Preserve user input, selection and navigation state across theme changes and recreation. Do not auto-enable dynamic colors if they override the selected app theme.
5. Keep XML and Compose aligned through existing theme adapters. Resolve colors at rendering/theme boundaries; do not cache resolved values across a theme change. Preserve Material component state/ripple behavior and the documented toggle-group background constraint. Use dp/sp and responsive layouts, support font scaling, RTL and long translations; never shrink important text just to hide clipping.
6. Use text labels or semantics as well as color to explain states. Verify contrast on actual light/dark surfaces, touch targets, TalkBack order and announced labels, keyboard focus, insets and gesture navigation. Decorative icons should not add duplicate announcements.
7. Inspect the changed diff for literal strings/colors/dimensions and duplicated role mappings, distinguishing legitimate canonical definitions and test fixtures. Compare the changed screen with an existing reference screen in both modes. Exercise switching theme while input is present, reopen the app, and inspect disabled/error states and Compose/View boundaries. Record screenshots/device evidence when available; mark visual verification pending if no renderer/device was available.

For visual exploration, use [UI UX Pro Max](../ui-ux-pro-max/SKILL.md) as suggestions under these app rules.
Official guidance:
- https://developer.android.com/develop/ui/views/theming/darktheme
- https://developer.android.com/develop/ui/compose/designsystems
- https://developer.android.com/guide/topics/resources/localization
