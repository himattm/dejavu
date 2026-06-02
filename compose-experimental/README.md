# compose-experimental

A staging area for Dejavu recomposition-tracking coverage of **experimental / newest-Compose
APIs**.

## Purpose

This module hosts Dejavu UI tests that verify recomposition tracking against Compose APIs that are
still experimental or only exist in the very latest Compose release. It is where coverage for new
APIs lands *first*, before those APIs graduate into the core accuracy suite.

## Why it is a separate module

`:dejavu`'s `commonTest` is compiled against the **full Compose BOM range** — back to 2024.06
(Compose 1.6) — in the `compose-compat` CI sweep. Experimental / newest-Compose APIs don't exist at
those older BOMs, so putting their tests in `:dejavu`'s `commonTest` would break that cross-version
sweep.

Isolating them here keeps the core suite portable across the whole BOM range. This module only ever
builds at the **current baseline BOM**, so it is free to reference APIs that older Compose versions
don't have.

## Currently covered

The module currently exercises Compose 1.11's new APIs:

- the experimental non-lazy `Grid` layout,
- the experimental `FlexBox` layout,
- `derivedMediaQuery` / `mediaQuery` adaptive breakpoints,
- the Styles API (`androidx.compose.foundation.style`),
- the experimental LinkBuffer composer runtime path
  (`ComposeRuntimeFlags.isLinkBufferComposerEnabled`), and
- `movableContentOf`.

These run on JVM, iOS, Wasm, and Android instrumented.

## Test style

Tests are **self-validating against a `SideEffect` ground truth**. Each tracked composable records a
composition from a `SideEffect` into a module-local `GroundTruth` counter (the Compose runtime
invokes `SideEffect` after every successful composition). After `resetRecompositionCounts()`, the
test snapshots a baseline; the `GroundTruth.delta(tag)` for a node then equals the number of
recompositions the runtime actually ran. The test asserts:

```kotlin
onNodeWithTag(tag).assertRecompositions(exactly = GroundTruth.delta(tag))
```

so the Dejavu tracer count is proven equal to the runtime's real recomposition count — not merely a
direction. The suite also includes deliberate **misconfiguration / over-recomposition** cases that
prove Dejavu reports the exact inflated count. (This mirrors the `SideEffect`-as-ground-truth
invariant in `:dejavu`'s own `SideEffectAccuracyTest`, but uses only Dejavu's public API since this
module cannot touch `DejavuTracer`.)

## Promotion checklist

When an experimental API graduates to **stable** AND the `:dejavu` BOM floor includes it, promote
its coverage out of this module:

1. Move the composable + its `SideEffect`-backed test into `dejavu/src/commonTest`.
2. Fold it into the accuracy suite — `ComposablePatternAccuracyTest` / `SideEffectAccuracyTest`.
3. Update the compatibility docs (`docs/how-it-works.md`, `README.md`).
4. Delete the now-redundant test (and any module-only helper it no longer needs) from here.
