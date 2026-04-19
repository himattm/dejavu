## 2024-05-18 - Unstable Class Parameter Causes Recomposition Regression
**Learning:** Found a performance bottleneck where a regular class (not a data class) `CartSummary` was used as a parameter. It used identity-based equality (Object.equals) so when its parent composed for unrelated reasons, the child composable `CartBanner` would incorrectly recompose.
**Action:** Changed `CartSummary` from a regular class to a `data class`. This ensures structural equality is used, allowing Compose to correctly skip recompositions when content is logically unchanged.
