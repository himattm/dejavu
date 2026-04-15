## 2024-04-15 - Compose Unstable Classes
**Learning:** Found an opportunity to optimize recomposition. In Compose, standard classes use reference equality. Even if their fields are identical, passing a new instance of a standard class as a parameter to a composable will cause it to recompose because the references differ.
**Action:** Always use `data class` for UI state objects passed to composables so Compose can perform structural equality checks and skip unnecessary recompositions when the data hasn't changed.
