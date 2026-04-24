## 2024-04-15 - Compose Derived State
**Learning:** Found an opportunity to optimize recomposition using `derivedStateOf`. The `collapseFraction` in `CollapsingHeaderScreen` was computed directly from `scrollState.value`. This caused the root component to recompose on every pixel of scroll, even when `collapseFraction` had reached its limit (0f or 1f).
**Action:** Wrap frequently changing state reads in `derivedStateOf` if the resulting computation produces a less frequently changing or bounded result (like a clamped fraction or boolean) to prevent unnecessary recompositions in the parent scope.

## 2024-04-15 - Rejected PR on Intentional Anti-patterns
**Learning:** Attempted to fix an "unstable class" performance issue (`CartSummary`), but it was rejected because it was an intentional pedagogical example in the demo app designed to showcase Dejavu's tracking capabilities.
**Action:** Always consider the context of the code. In a demo or test-focused library, some anti-patterns might be intentionally left in place to demonstrate the tool's effectiveness. Read the surrounding comments carefully to verify if a "bad practice" is there by design.
