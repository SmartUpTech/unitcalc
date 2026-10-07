---
name: calculator-logic
description: Core logic for mathematical evaluation, unit conversions, and the calculator registry. Use this when adding new units, fixing parsing bugs, or modifying calculation behavior.
---

# CALCULATOR SYSTEM

## EVALUATION PIPELINE
1. **Parsing**: Converts raw string input into an expression tree.
2. **Precedence**: Handles operator priority (e.g., multiplication before addition).
3. **Precision**: Ensures consistent decimal handling.
4. **Result**: Produces a `CalculationHistoryItem` for storage.

## DOMAIN MODELS
- **Unit**: Represents physical quantities (Length, Mass, etc.).
- **Currency**: Handles monetary conversions with rate caching.
- **Conversion**: Manages the state and logic of a specific conversion session.

## IMPLEMENTATION RULES
- **Reactive Calculation**: Results should update immediately as input changes (where applicable).
- **Error Handling**: Gracefully handle Division by Zero, Syntax Errors, and Overflow.
- **Features**: Support sign toggling, percentages, and scientific functions (sqrt, powers).
- **Graphy Integration**: Logic must produce structured snapshots for visual rendering.

## REGISTRY
- **CalculatorCatalog**: The central source of truth for all tools. New categories, units, or calculators MUST be registered here to appear in the UI.

