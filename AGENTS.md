# AI AGENT GLOBAL
- Context: Read only relevant docs. Min files.
- Logic: Follow architecture. Reuse components/utils. No duplication.
- UI: Use theme/design system. No hardcoding. Consistency required.
- Task: Scoped changes only. No unrelated refactor.
- Optimization: Low context consumption. Min file reading.

## TOOL SCREEN UX — MANDATORY
- Keep every calculator, converter and utility tool on one non-scrolling screen within the actual available viewport. Inputs, results, selectors, essential actions and keypad must be usable together without scrolling the screen or an embedded content panel.
- Account for system bars, app bars, tabs, navigation and IME. Adapt spacing, arrangement and secondary content; never hide overflow, clip controls, shrink text illegibly or add a scrolling fallback. If an extreme accessibility configuration cannot fit, report the unresolved design constraint instead of claiming compliance.
- Keep keypad look and feel consistent across the app: shared components/styles, typography, semantic key colors, shapes, spacing, icons, touch targets and interaction feedback. Different tools may need different keys, but must not introduce their own keypad visual style.
- Verify constrained viewports, font/display scaling and all themes. Source inspection alone does not establish visual or runtime compliance. Follow docs/DESIGN_SYSTEM.md and the UI/theme skills below.

## ROUTING
Paths below are relative to docs/. Confirm implementation when a document is stale.
- UI/Layout -> DESIGN_SYSTEM.md
- Theme/Typography -> THEME.md
- Logic/Parsing/Units -> CALCULATOR_SYSTEM.md
- Graphy/Visualization -> GRAPHY_ENGINE.md
- Architecture/State -> ARCHITECTURE.md
- Settings/Prefs -> SETTINGS.md
- Storage/Database -> DATA_STORAGE.md
- Task Checklists -> WORKFLOWS.md
- Terms -> GLOSSARY.md
- Testing -> TESTING.md
- Perf -> PERFORMANCE.md

## PROTOCOL
1. Analyze task. 2. Load relevant docs. 3. Target files. 4. Implement/Validate.

## SHARED SKILLS
Keep reusable skills in .agents/skills/ as the single source of truth. Agents that do not auto-discover this directory must follow this routing and open the linked SKILL.md manually; directory naming alone does not guarantee every tool's automatic discovery.

- Android feature/state/navigation/integration -> [.agents/skills/android-development/SKILL.md](.agents/skills/android-development/SKILL.md)
- Theme/resources/light-dark/visual consistency -> [.agents/skills/android-theme-consistency/SKILL.md](.agents/skills/android-theme-consistency/SKILL.md)
- Calculations/units/currency/precision/result correctness -> [.agents/skills/calculator-accuracy/SKILL.md](.agents/skills/calculator-accuracy/SKILL.md)
- UI/UX/accessibility -> [.agents/skills/ui-ux-pro-max/SKILL.md](.agents/skills/ui-ux-pro-max/SKILL.md)
- Startup/jank/memory/battery/shrinking -> [.agents/skills/android-performance/SKILL.md](.agents/skills/android-performance/SKILL.md)
- Manifest/privacy/SDK/release/Google Play policy -> [.agents/skills/google-play-compliance/SKILL.md](.agents/skills/google-play-compliance/SKILL.md)
- Ads/consent/UMP/monetization -> [.agents/skills/admob-compliance/SKILL.md](.agents/skills/admob-compliance/SKILL.md)

Load only skills needed for the current task and references needed for the specific issue. Use local search scripts instead of loading UI CSV catalogs. Existing project docs and user requirements govern app-specific theme and architecture choices. Verify live official policy sources before applying time-sensitive rules. Skills guide future work; their presence does not certify this app's performance or compliance.

For Android implementation tasks, load android-development and only the specialist skills triggered by the change. Match result correctness, responsiveness, shared theme behavior and navigation continuity to the existing app. Do not sacrifice accuracy or user data for speed. Do not bulk-read every skill, CSV, generated file or source directory.

## GRAPHY CONSISTENCY
New calculators and converters MUST use existing Graphy primitives. Introduce a new
visual primitive only for a genuinely new semantic concept that existing components
cannot represent. Calculators/converters supply content; the shared engine owns
colors, shapes, typography, spacing, connectors, hierarchy and layout.
See docs/GRAPHY_ENGINE.md before implementing Graphy.
