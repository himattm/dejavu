## 2024-05-02 - Unstable Compose State

**Learning:** Using a regular `class` instead of a `data class` for state passed to a Jetpack Compose component breaks Compose's structural equality checks (using reference equality instead). This causes unnecessary recompositions whenever the parent recomposes.

**Action:** Always use `data class` for models passed as Jetpack Compose state parameters to enable structural equality checking and proper recomposition skipping.
