## 2025-05-18 - Structural Equality for Composable Parameters
**Learning:** Using regular classes for parameters in Jetpack Compose passes them by reference identity. When a parent recomposes, a new object instance is created even if the data inside is logically identical, triggering unnecessary recompositions in child composables because Compose thinks the input changed.
**Action:** Always use `data class` for state objects passed as parameters to ensure structural equality (`equals()`) instead of reference equality, thereby preventing unnecessary recompositions.
