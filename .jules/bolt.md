## 2026-04-16 - Compose Recomposition and `data class`
**Learning:** Compose uses identity-based equality (`Object.equals`) for regular classes, causing unnecessary recompositions when a new instance is passed, even if its fields are identical. Using a `data class` provides structural equality, allowing Compose to skip recomposition if the logical content hasn't changed.
**Action:** Always prefer `data class` over `class` for data-holding parameters passed to Composables to prevent performance regressions via needless recompositions.
