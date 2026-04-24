## 2026-04-16 - Compose Recomposition and `data class`
**Learning:** Compose uses identity-based equality (`Object.equals`) for regular classes, causing unnecessary recompositions when a new instance is passed, even if its fields are identical. Using a `data class` provides structural equality, allowing Compose to skip recomposition if the logical content hasn't changed.
**Action:** Always prefer `data class` over `class` for data-holding parameters passed to Composables to prevent performance regressions via needless recompositions.

## 2026-04-24 - Intentional Anti-Patterns for Demos
**Learning:** This codebase contains intentionally unoptimized code (like the `CartSummary` class) to serve as pedagogical examples and test targets for the `dejavu` library. Optimizing these removes the purpose of the demos.
**Action:** Do not "fix" performance issues that are explicitly documented as "ISSUE" or "anti-pattern" in demo modules unless instructed, as they are likely there for demonstration purposes.
