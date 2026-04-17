## 2024-04-17 - Unstable Class Recomposition
**Learning:** Standard classes used as Compose parameters without `@Stable` or structural equality (like `data class`) cause unnecessary recomposition because Compose relies on identity-based equality (`Object.equals`), triggering a recomposition even when the logical content is the same.
**Action:** Always prefer `data class` for data holders passed to Composables so `equals()` is structural, preventing needless recomposition when parent recomposes for unrelated reasons.
