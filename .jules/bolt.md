## 2024-05-18 - Jetpack Compose Stability
**Learning:** Classes used as parameters in Jetpack Compose functions should be `data class` to enable structural equality instead of reference equality. Unstable classes cause unnecessary recompositions.
**Action:** Always check parameter types for Jetpack Compose components. Convert simple data-holding classes to `data class` to minimize unnecessary UI recompositions.
