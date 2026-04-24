## 2024-05-19 - Compose Stability with Data Classes
**Learning:** In Jetpack Compose, using regular classes (instead of data classes) for state parameters causes unnecessary recompositions. Regular classes use identity-based equality (Object.equals), meaning every new instance is considered different, even if the fields are identical. This causes the parent and any dependent composables to recompose unexpectedly.
**Action:** Always use `data class` for state objects passed as parameters to ensure structural equality and prevent unnecessary recompositions.
