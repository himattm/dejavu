## 2024-04-21 - Compose data class equality
**Learning:** Using regular classes instead of data classes for UI state in Jetpack Compose causes unnecessary recompositions because Compose uses reference equality by default.
**Action:** Always use data classes for objects passed as parameters to Compose components to enable structural equality and allow Compose to skip recomposition when content hasn't changed.
