## 2024-05-06 - Fixing Unstable Class Parameter issue in Compose
**Learning:** Found that non-data classes can cause Compose recomposition regressions. By changing it to a data class, Compose leverages structural equality checks and prevents unnecessary recompositions when a parent recomposes and creates a new object with the same logic.
**Action:** Always favor data classes over normal classes for Compose state variables to maintain stability and avoid wasteful recompositions.
