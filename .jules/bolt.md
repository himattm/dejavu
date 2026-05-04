## 2024-05-19 - [Compose Stability]
**Learning:** Passing a regular class as a parameter in Compose causes unnecessary recompositions because Compose uses `Object.equals` (identity-based) for non-data classes, while `data class` enables structural equality and skipping.
**Action:** Always use `data class` for state objects passed as parameters to ensure structural equality and prevent unnecessary recompositions.
