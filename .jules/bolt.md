## 2024-04-20 - Unstable Class Parameters Cause Recomposition Regressions
**Learning:** Regular classes used as parameters in Compose use identity-based equality (Object.equals). Even if the logical content is identical, a new instance is created on parent recomposition, causing children to needlessly recompose.
**Action:** Always use `data class` for state holding parameters in Compose to ensure structural equality is used, allowing Compose to skip unnecessary recompositions.
