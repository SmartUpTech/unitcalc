---
name: ui-ux-pro-max
description: Design and review Android UI/UX for Java/XML and Kotlin/Jetpack Compose, using local UI UX Pro Max datasets for styles, colors, typography, accessibility, interactions, and charts. Use for screen design, UI fixes, theme consistency, navigation, or accessibility.
---

# UI UX Pro Max — Android

## Calculator+ tool-screen acceptance

Enforce AGENTS.md's mandatory Tool Screen UX rules for calculators, converters and utilities. Keep the complete primary workflow in one available viewport, with no page-level or embedded-content scrolling, including fallback layouts. Audit ScrollView/NestedScrollView, scroll modifiers and lazy lists used as screen containers; do not confuse an input control's picker gesture with scrolling the tool screen. Measure after persistent chrome/insets. Reflow and compact using shared tokens, preserving readable text and accessible controls; never disable scrolling while leaving unreachable overflow. Report physically impossible configurations as unresolved. Compare keypads across tools in the same theme and viewport; require the shared visual and interaction language in the theme skill.

## Workflow

1. Read the root AGENTS.md and only the relevant docs/DESIGN_SYSTEM.md, docs/THEME.md, or docs/GRAPHY_ENGINE.md. Preserve existing theme tokens, components, and app behavior; treat search results as suggestions.
   For resource rules, semantic roles and light/dark behavior, also follow [android-theme-consistency](../android-theme-consistency/SKILL.md); for navigation/state changes follow [android-development](../android-development/SKILL.md).
2. Detect whether the target screen uses XML Views, Compose, or interoperability. Keep its current framework unless the task requests migration. Use resource strings and existing dimensions/styles; support all existing languages.
3. Search one concern with 2–5 meaningful terms. Run from the repository root (or resolve an absolute repository path first):
   ```bash
   python3 .agents/skills/ui-ux-pro-max/scripts/search.py "touch accessibility" --domain ux
   python3 .agents/skills/ui-ux-pro-max/scripts/search.py "state recomposition" --stack jetpack-compose
   python3 .agents/skills/ui-ux-pro-max/scripts/search.py "comparison proportions" --domain chart
   ```
   Use python or py -3 if python3 is unavailable. Require Python 3; these scripts have no third-party dependencies.
4. For a requested new visual system, use:
   ```bash
   python3 .agents/skills/ui-ux-pro-max/scripts/search.py "calculator utility minimal" --design-system -f markdown
   ```
   Adapt output to Android and existing design rules. Do not apply generated CSS, web layouts, or GSAP to native screens. Do not replace the app's theme merely because a search suggests another palette.
5. Inspect result fit. Retry once with a narrower query if empty or unrelated; label general recommendations as fallback. Never fabricate a dataset match.
6. Read [quick-reference.md](references/quick-reference.md) for the relevant rule category and [pro-rules.md](references/pro-rules.md) before delivering UI. Translate web-specific guidance into native equivalents: dp/sp, semantics/contentDescription, TalkBack, safe insets, and Android touch targets of at least 48dp.
7. Verify affected light/dark themes, supported languages, large font sizes, keyboard/insets, screen sizes, contrast, touch feedback, and lifecycle behavior. Report checks actually performed and any device checks still needed.

## Bundled scope and provenance

Carry forward the skill scripts, references, and Android-relevant datasets from SmartUpTech/unitcalc main; include upstream chart and landing data needed by search/design-system generation. Bundle domains style, color, chart, landing, product, ux, typography, icons, gsap, and web (native/app guidance); only jetpack-compose is bundled as a stack. For Java/XML, apply native guidance to existing resources instead of using an unrelated stack. Other stacks, react, and google-fonts require additional upstream datasets and are outside this installation.

Upstream: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill
Preserve [LICENSE](LICENSE). Resolve data relative to the script location; keep datasets out of routine context and use the search tool instead of reading all CSV files.
