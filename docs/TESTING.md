---
name: testing
description: Guidelines for writing and running unit and UI tests. Use this when adding new tests or verifying changes via the test suite.
---

# TESTING STRATEGY

## CATEGORIES
- **Unit Tests (`src/test`)**: Business logic, mathematical models, and utility classes.
- **UI Tests (`src/androidTest`)**: Fragment interactions, navigation flows, and theme propagation.

## TARGETS
- **Models**: Validate `Conversion` and `Unit` logic.
- **Calculator**: Verify parsing accuracy and edge case handling.
- **Catalog**: Ensure all entries are correctly registered and localized.

## BEST PRACTICES
- **Framework**: Use JUnit 4 and Mockito for dependency isolation.
- **Isolation**: No network or side effects in unit tests.
- **Execution**: Run `./gradlew test` for local verification.

