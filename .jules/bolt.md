## 2024-05-18 - Use data classes for Jetpack Compose state objects
**Learning:** Jetpack Compose recomposition uses structural equality. If a regular class (which uses identity-based equality via `Object.equals`) is used as a parameter for a Composable, the Composable will unnecessarily recompose every time a new instance of the class is created, even if all logical fields are identical.
**Action:** Always use `data class` for state objects passed to Composables so that their `equals()` method compares structural content.
