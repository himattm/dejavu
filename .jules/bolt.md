## 2024-05-18 - Making State Objects Data Classes for Structural Equality
**Learning:** In Compose, regular classes passed as parameters cause recomposition when the parent recomposes, because their reference changes even if the content is functionally the same (due to identity-based equality via `Object.equals`).
**Action:** Use `data class` for state objects passed as parameters to ensure structural equality (`equals()`), preventing unnecessary recompositions.
