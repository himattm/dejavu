## 2024-04-17 - Unstable Class Recomposition
**Learning:** Standard classes used as Compose parameters without `@Stable` or structural equality (like `data class`) cause unnecessary recomposition because Compose relies on identity-based equality (`Object.equals`), triggering a recomposition even when the logical content is the same.
**Action:** Always prefer `data class` for data holders passed to Composables so `equals()` is structural, preventing needless recomposition when parent recomposes for unrelated reasons.

## 2024-04-17 - The Demo Codebase Optimization Trap
**Learning:** Sometimes what looks like an obvious optimization target (e.g. converting a class to a `data class` to enable structural equality and prevent unnecessary recompositions) is actually a pedagogical example intentionally placed to demonstrate a problem.
**Action:** Always read the surrounding comments carefully before attempting an optimization. If a component is deliberately documented as demonstrating an anti-pattern or bug, do NOT attempt to "fix" it without consulting the user, as fixing it removes its educational purpose.
