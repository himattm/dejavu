## 2025-05-05 - Unstable Class Parameters Cause Unnecessary Recompositions
**Learning:** Using a regular class for state objects passed as parameters to Composable functions causes unnecessary recompositions. This is because regular classes use identity-based equality (Object.equals) instead of structural equality, meaning Compose sees a new reference as a different parameter even if the logical content is identical.
**Action:** Always use `data class` for state objects passed as parameters to Composable functions to ensure structural equality and prevent needless recompositions.
