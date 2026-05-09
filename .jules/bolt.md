## 2026-04-28 - Initializing Bolt Journal
**Learning:** Establishing the journal format as required by constraints.
**Action:** Always document critical learnings specific to this codebase's architecture and performance patterns here.
## 2026-04-28 - Recomposition Analysis
**Learning:** Found two clear recomposition bottlenecks: (1) `ProductHeader` uses an `Int` instead of a `Boolean` causing unnecessary recompositions on every increment, and (2) `CartSummary` is a regular class causing identity-based equality checks which trigger unnecessary recompositions on any parent update.
**Action:** (1) Change `ProductHeader` to accept a `Boolean` (e.g. `hasSelection`), (2) Convert `CartSummary` to a `data class`.
