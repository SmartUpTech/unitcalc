# Appearance customisation

Settings → Appearance → Customise keeps the existing Theme selector in place,
then adds Buttons and Text Font. Number format remains in its existing section.
Theme palettes, theme IDs, availability, reward handling and selection are unchanged.

`Appearance` owns stable style/shape/font IDs using the existing `Preferences`
store. Missing or invalid values resolve to Flat, Rounded and Oswald. Shape is
stored independently of style, including Flat, so it is never reset on a style
change. Buttons and font dialogs keep saveable drafts; only Apply writes storage.
Reset removes only those three preferences and retains the theme and other settings.

The Buttons popup has one live row (number, function, operator, backspace and
Equal). Button Style and Button Shape each use a horizontal strip of text choices
with an explicit selected check and outline. Both strips reuse one component,
bring the selected choice into view and update that single preview immediately.
Flat disables shape choices while preserving the remembered shape. Only the
popup may scroll vertically in constrained windows; tool layouts are unchanged.

Text Font uses the same preview row and horizontal selector. Each choice's name
uses its own regular font. Selecting a choice updates the native key labels,
“This is <font name>” text and mathematical sample together, without writing the
font preference. The preview retains the applied button style, remembered shape
and theme. Apply saves the selected font; Cancel, Back and outside taps discard
the draft. Drafts survive saved-state restoration like the Buttons dialog.

`KeyAppearanceDrawable` is shared by real numeric keys, Equal, BMI Done and the
native Views inside Compose previews. Shapes fit within each existing key's touch
bounds; Circle uses the smaller dimension for its diameter. Style spacing stays
inside those bounds, so tool layout geometry and click listeners are preserved.
The Equal role uses accent fill and readable on-accent ink even for Flat/Outline.
Press ripples use the same geometry and semantic foreground. Gradients and edge
shading are limited to the finish styles.

`AppearanceTypography` handles the View boundary (including dynamic rows) and
Canvas font lookup; GraphyTheme supplies the same bundled family to Compose.
Android retains its language fallback. Fonts are bundled for offline use; their
OFL licenses are under docs/licenses. Oswald remains the default.

Regression coverage:

- AppearanceTest: defaults, invalid values, independent shape persistence, reset
  isolation, all theme/style/shape/role renders, contrast, circle geometry and
  shared Equal/BMI treatment. Native renders write Equal/BMI review sheets.
- AppearanceUiTest: draft selection, Flat round trips, Apply/Cancel/reopen,
  dismissal, and light/dark popup/font/style/shape review captures.
- AppTypographyTest: all five fonts across every Material typography role.

Review images are generated under `.artifacts/appearance/` by the native-rendered
Robolectric tests. They are renderer evidence, not physical-device certification.
Existing extreme viewport constraints documented in DESIGN_SYSTEM.md still apply.
