---
name: workflows
description: High-level overview of common development workflows. For detailed step-by-step guides, refer to specialized skills like 'new-converter'.
---

# DEVELOPMENT WORKFLOWS

## ADDING FEATURES
- **New Unit/Category**: Define physical constants, update UI strings, and register in `CalculatorCatalog`.
- **New Theme**: Add semantic colors to `colors.xml` and register in `CalculatorThemes`.
- **New Workspace Pane**: Create XML layout, update `Fragment` registry, and bind page logic.

## QUALITY CONTROL
- **Style**: Follow `docs/DESIGN_SYSTEM.md` and `.agents/skills/android-theme-consistency/SKILL.md`.
- **Testing**: Verify logic with unit tests.
- **Verification**: Follow `docs/TESTING.md`; record actual build, test and device results with remaining limitations.

## MOVING A TOOL BETWEEN GROUPS
- Follow the source routing in `AGENTS.md` and package boundaries in `ARCHITECTURE.md`.
- Update source/test imports and package declarations, fully qualified reflection references, XML custom views and manifest components together.
- Preserve catalog IDs, stored history/preferences, resource IDs, calculation behavior and shared appearance owners.
- Build the APK, compile/run affected tests and open moved destinations on a device. A production compile alone does not validate test packages or runtime XML inflation.

