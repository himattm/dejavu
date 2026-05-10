## 2024-05-18 - Jetpack Compose Stability with Data Classes
**Learning:** Regular Kotlin classes use identity-based equality, causing Compose to falsely detect changes when instances are recreated with identical data, triggering needless recompositions. Using `data class` enables structural equality which fixes this issue.
**Action:** Always use `data class` for objects passed into Jetpack Compose UI parameters unless identity equality is intentionally required.
