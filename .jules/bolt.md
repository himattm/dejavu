## 2024-05-18 - Unstable Class Parameter Causes Recomposition Regression
**Learning:** Found a performance bottleneck where a regular class (not a data class) `CartSummary` was used as a parameter. It used identity-based equality (Object.equals) so when its parent composed for unrelated reasons, the child composable `CartBanner` would incorrectly recompose.
**Action:** Changed `CartSummary` from a regular class to a `data class`. This ensures structural equality is used, allowing Compose to correctly skip recompositions when content is logically unchanged.
## 2024-05-18 - Attempted to Optimize Intentional Pedagogical Example
**Learning:** Found a performance bottleneck (unstable class parameter `CartSummary`) but it turned out to be an intentional pedagogical example in the demo app to illustrate recomposition anti-patterns.
**Action:** When working in test/demo projects (especially ones designed to test a library that catches recomposition regressions like Dejavu), verify that the "bottleneck" isn't intentionally left there for demonstration purposes. Read surrounding comments carefully before optimizing.
