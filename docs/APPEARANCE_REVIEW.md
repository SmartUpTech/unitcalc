# Appearance review

Theme's existing selector, catalog, availability and unlock flow are unchanged.
Number format and calculation/conversion code are unchanged.

## Screenshots

These are native Android renders produced by the production preview/key renderer
and the production Compose dialogs under Robolectric (API 34), not mockups or
physical-device screenshots.

| Review | Light | Dark |
| --- | --- | --- |
| Buttons popup | [Light](appearance-review/buttons-light.png) | [Dark](appearance-review/buttons-dark.png) |
| All eight styles and four shapes | [Light](appearance-review/styles-shapes-light.png) | [Dark](appearance-review/styles-shapes-dark.png) |
| Shape selection in popup | [Light](appearance-review/shapes-light.png) | [Dark](appearance-review/shapes-dark.png) |
| Text Font selection | [Light](appearance-review/fonts-light.png) | [Dark](appearance-review/fonts-dark.png) |
| Equal and BMI Done across all styles | [Light](appearance-review/equal-bmi-light.png) | [Dark](appearance-review/equal-bmi-dark.png) |

Additional constrained-screen capture: [320dp at 2× text](appearance-review/buttons-large-text.png).

The Buttons popup now uses exactly one five-key live preview row. Style and shape
choices are separate horizontal strips with selected checkmarks, outlines and
scroll indicators. Both selectors update the same preview without saving until
Apply; Flat still disables shape choices and remembers the last shape.
The comparison sheets are review-only, not additional previews in the popup.

The style sheets use Rounded for comparisons and include a separate four-shape
section. Equal remains accent-filled in every style; the Equal/BMI sheets use
Circle to demonstrate the remembered shape in Flat.

## Validation scope

Latest revalidation (2026-10-30): **52 targeted tests passed, zero failures**.
Debug assembly succeeded and lint completed with **0 errors, 406 warnings**.
This includes new checks for actual system Back dispatch and outside down/up
pointer events discarding drafts, saved-state draft restoration, applied choices
being visible after RTL reopening, and every font fitting the single preview row
with Apply/Cancel visible at 2× text. No production-code fixes were required.
Theme selector/access/preferences files match release/v11, and Number format still
has one settings section.

- Original feature: 47 focused regression tests passed.
- Compact popup follow-up: debug APK assembly and 11 focused tests passed, including
  every style/shape selection updating the only preview row, Apply/Cancel and
  remembered shape. Android lint passed (0 errors, 406 warnings).
- All registered themes, eight styles, four shapes and five key roles render;
  foreground contrast and circle geometry checked.
- Draft selection, Apply, Cancel, dismissal, reopening and Flat shape memory.
- Font defaults, selection, reset isolation, invalid preference fallback and all
  Material typography roles. Numeric key text and touch bounds at a 360×600 tool
  viewport with 1.0/1.3 font scale, across all five fonts.
- Popup captures at 320dp width with 1.0/1.3/2.0 text scale, using Compose UI test synchronization.
- Existing theme preference/access/contrast, BMI input/state restoration and
  calculator/converter layout regression checks.

Physical-device testing, a real process kill/relaunch, TalkBack and minimum-API
font fallback remain manual checks. The existing extreme tool viewport limits in
DESIGN_SYSTEM.md (320×400 at 1.3 text and the documented 520dpi currency viewport)
remain unresolved; this change preserves the layouts and adds no tool scrolling.
