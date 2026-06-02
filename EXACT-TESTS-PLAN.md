# Plan: Make recomposition assertions exact across the legacy test suites

**Status:** Not started (deferred). Created 2026-06-02.
**Goal:** Eliminate weak/directional recomposition assertions in the legacy `dejavu` pattern tests
and `demo` instrumented tests so every test either (a) pins an exact behavioral budget
(`exactly = N` / `assertStable()`), or (b) self-validates Dejavu's count against a `SideEffect`
ground truth (`tracer == SideEffect delta`) — which is exact even when the absolute count is
non-deterministic. Two narrow classes stay directional by design (see Exceptions).

## Why
`assertRecompositions(atLeast = 1)` passes whether a node recomposed once or fifty times, so it
proves Dejavu *detects* a recomposition but not that it *counts correctly*. `atLeast = 0` is
vacuous. The `compose-experimental` module was already converted to the exact/ground-truth pattern
(see "Reference assets"); this plan extends that rigor to the rest of the suite.

## Current state (audit 2026-06-02)
- `compose-experimental`: **0 directional** — already 100% exact. Not in scope.
- `dejavu/src/commonTest`: **112** directional `assertRecompositions(atLeast|atMost ...)` calls.
- `demo/src/androidTest`: **101** directional calls.

### Per-file inventory (directional call counts)
`dejavu/src/commonTest/kotlin/dejavu/`:
StarRatingPatternTest 19 · DonutChartPatternTest 8 · ExpandableCardPatternTest 8 ·
ReorderListPatternTest 7 · DeepNestingStressPatternTest 7 · AssertionApiPatternTest 7 (KEEP) ·
ChipFilterPatternTest 6 · KeyIdentityPatternTest 6 · AdvancedPatternTest 5 · SwipeListPatternTest 5 ·
CollapsingHeaderPatternTest 4 · ToggleMorphPatternTest 4 · PagerCrossfadePatternTest 4 ·
DialogPopupPatternTest 4 · AnimationPatternTest 4 (mostly KEEP) · LazyListStressPatternTest 4 ·
LazyVariantsPatternTest 2 · ImplicitTrackingPatternTest 2 · SubcomposePatternTest 2 ·
InputScrollPatternTest 2 · FlowStatePatternTest 1 · LibraryCorrectnessPatternTest 1

`demo/src/androidTest/java/demo/app/`:
StarRatingTest 9 · ReorderListTest 9 · DonutChartTest 8 · AssertionApiTest 7 (KEEP) ·
DejavuLibraryCorrectnessTest 6 · RecompositionRegressionTest 6 · ChipFilterTest 6 ·
DeepNestingStressTest 6 · PerTagTrackingRegressionTest 4 · PagerCrossfadeTest 4 · DialogPopupTest 4 ·
CollapsingHeaderTest 4 · ToggleMorphTest 4 · LazyListStressTest 4 · AnimationStressTest 3 (KEEP) ·
InputScrollTest 3 · SubcomposeTest 3 · SwipeListTest 3 · ImplicitTrackingTest 2 ·
AdvancedPatternsTest 2 · LazyVariantsTest 2 · ExpandableCardTest 2

## Exceptions (stay directional — do NOT convert)
1. **Assertion-API coverage tests** — `AssertionApiPatternTest` (~7) and demo `AssertionApiTest` (~7).
   These exist to exercise the `atLeast`/`atMost`/range modes and their error messages
   (`rangeMode_withinRange_passes`, `atMostError_containsDescriptiveMessage`, `negativeAtLeast_throws`,
   …). You cannot test the `atMost` API without calling `atMost`. Leave as-is.
2. **Continuously-running / infinite animations** — e.g. `AnimationPatternTest.infiniteTransition_*`
   and the `atLeast = 0` smoke checks already documented with the comment
   *"exact count depends on Compose animation internals across platforms."* The count is a moving
   target while frames render; an exact equality is racy. Keep a range, OR stop/settle the animation
   before reading counts and only then assert ground-truth equality. Decide per test during execution.

## Approach by category
For every non-exception test, classify each assertion and convert:

- **Deterministic count** (discrete state changes: clicks, toggles, filters, reorders, ratings) →
  `assertRecompositions(exactly = N)` / `assertStable()`. Derive `N` from a `SideEffect` ground truth
  so it can't go stale (don't hand-guess). Most of StarRating, ChipFilter, ReorderList, KeyIdentity
  (non-`atMost`), DeepNesting, Advanced, Subcompose, DialogPopup, FlowState, ImplicitTracking,
  LazyVariants, LibraryCorrectness, demo Recomposition/PerTag/LibraryCorrectness fall here.
- **Non-deterministic but settle-able** (gestures, scroll, finite animations: Swipe, Pager,
  CollapsingHeader, InputScroll, LazyListStress, ToggleMorph, ExpandableCard, DonutChart) →
  `waitForIdle()` to settle, then assert `exactly = GroundTruth.delta(tag)` (tracer == real count).
  Exact in the accuracy sense without hardcoding a flaky literal. Where a node should be untouched,
  also assert `assertStable()`.
- **`atMost = N` upper-bound guards** (e.g. KeyIdentity loop items) → keep the over-recompose guard
  semantics; convert to `exactly = N` ONLY if the count is provably deterministic (== via ground
  truth). If a node legitimately recomposes 0-or-1 across platforms, `atMost = 1` is the correct,
  non-flaky assertion — keep it.

## Per-module mechanics
- **`dejavu/src/commonTest`**: reuse the EXISTING internal ground-truth machinery — the `groundTruth`
  object + `assertAccuracy(qualifiedName)` / `assertTagAccuracy(tag, qualifiedName)` helpers and
  `DejavuTestContent` / `enableDejavuForTest` / `refreshTagMapping` (see
  `ComposablePatternAccuracyTest.kt`). This module can call `DejavuTracer` directly. Prefer adding a
  `SideEffect`-instrumented variant of each pattern composable (mirroring `SideEffectAccuracyTest.kt`,
  invariant `dejavuCount == sideEffectCount - 1`) and asserting exact.
- **`demo/src/androidTest`**: the demo app CANNOT see `dejavu`'s internal test helpers. Use the public
  assertion API (`onNodeWithTag(...).assertRecompositions(exactly = ...)` via the
  `createRecompositionTrackingRule` rule) backed by a demo-local `SideEffect` ground-truth helper
  analogous to `compose-experimental/.../GroundTruth.kt` (record/snapshotBaseline/delta/clear).

## Reference assets (copy the pattern from these)
- `compose-experimental/src/commonTest/kotlin/dejavu/experimental/GroundTruth.kt` — public-API ground
  truth helper (record / snapshotBaseline / delta / clear).
- `compose-experimental/src/commonTest/kotlin/dejavu/experimental/ExperimentalLayoutRegressionTest.kt` —
  worked example: `SideEffect { GroundTruth.record(tag) }` per node, `snapshotBaseline()` after
  `resetRecompositionCounts()`, `assertRecompositions(exactly = GroundTruth.delta(tag))`, plus
  deliberate misconfiguration over-recompose tests.
- `dejavu/src/commonTest/kotlin/dejavu/SideEffectAccuracyTest.kt` — canonical invariant
  `dejavuCount == sideEffectCount - 1`.
- `dejavu/src/commonTest/kotlin/dejavu/ComposablePatternAccuracyTest.kt` — `groundTruth` object +
  `assertAccuracy` / `assertTagAccuracy` helpers (internal access).

## Stretch (optional, mirrors the compose-experimental work)
While converting, add a deliberate **misconfiguration / over-recomposition** case to the
highest-value patterns (counter, chip filter, reorder, deep nesting) proving Dejavu reports the exact
inflated count — same shape as the compose-experimental misconfig tests.

## Verification (run after each module's conversion; gradle with `-q --console=plain`)
```
./gradlew -q --console=plain :dejavu:jvmTest
./gradlew -q --console=plain :dejavu:iosSimulatorArm64Test
./gradlew -q --console=plain :dejavu:wasmJsBrowserTest
./gradlew -q --console=plain :dejavu:testDebugUnitTest
./gradlew -q --console=plain :demo:connectedDebugAndroidTest          # emulator required
./gradlew -q --console=plain apiCheck
```
Each converted test must pass on ALL platforms — an `exactly`/ground-truth assertion that fails on
one platform is a real platform-specific tracking bug (fix in library code, don't loosen).

## Risks
- **Flakiness** on continuous animations — handle via the Exceptions rule (settle or keep range).
- **Multi-instance tag mapping** — for composables reused across tags, exact per-tag counts depend on
  `getPerTagRecompositionCount`; use `assertTagAccuracy` (dejavu) / per-tag deltas (demo).
- **Dispatcher** — suite is on Compose testing v2 (StandardTestDispatcher); counts were verified
  unshifted vs v1, but re-confirm after conversion.
- **Scope** — ~40 files. Recommend doing `dejavu/commonTest` first (library's own coverage), then
  `demo/androidTest`. Convert one file at a time and run its target to localize regressions.
