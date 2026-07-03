# How It Works

Dejavu hooks into the Compose runtime's `CompositionTracer` API (available since compose-runtime 1.2.0):

1. **Intercepts trace calls** — `Composer.setTracer()` receives callbacks for every composable enter/exit
2. **Maps testTag to composable** — walks the `CompositionData` group tree to find which composable encloses each `Modifier.testTag()`
3. **Counts recompositions** — maintains a thread-safe counter per composable, incrementing on recomposition (not initial composition)
4. **Tracks causality** — `Snapshot.registerApplyObserver` detects state changes; dirty bits detect parameter-driven recompositions
5. **Reports on failure** — assembles source location, timeline, tracked composables, and causality into a structured error

All tracking runs in the app process on the main thread, directly accessible to instrumented tests.

## Compatibility

**Minimum supported Compose: 1.10 (BOM 2026.01.01).** Compose 1.10 is the first version with the
`CompositionObserver` API that Dejavu's causality diagnostics depend on. For older Compose (1.6–1.9),
use Dejavu 0.3.x. Requires Kotlin 2.1+ with the Compose compiler plugin.

| Compose BOM | Compose | Kotlin | Status |
|---|---|---|---|
| 2026.01.01 | 1.10.x | 2.1.x+ | Minimum |
| 2026.03.01 | 1.10.x | 2.1.x+ | Tested |
| 2026.06.00 | 1.11.x | 2.3.x+ | Baseline |

The baseline is Compose 1.11 (BOM 2026.06.00); the floor is Compose 1.10 (BOM 2026.01.01). CI runs a
`compose-compat` matrix that compiles and unit-tests across 2026.01.01, 2026.03.01, and 2026.06.00,
and the Android instrumented gates run the same three BOMs. `CompositionObserver` support is
unconditional — there is no degraded / observer-excluded build path.

## Compose Testing v2

Dejavu's test harness uses the Compose testing **v2** APIs (`runComposeUiTest` /
`createAndroidComposeRule` from the `androidx.compose.ui.test.v2` packages). These default to
`StandardTestDispatcher` rather than v1's `UnconfinedTestDispatcher`. Recomposition counts were
verified to be unchanged on JVM under the new dispatcher, so no rebaselining of test expectations
was required.

## Compose 1.11 Coverage

The `compose-experimental` module — a staging area for recomposition coverage of experimental /
newest-Compose APIs that can't live in `:dejavu`'s commonTest (which compiles against the full
supported Compose BOM range, 2026.01.01 → 2026.06.00) — exercises Dejavu against Compose 1.11's new composables and
runtime paths: the experimental `Grid` and `FlexBox` layouts, `derivedMediaQuery` / `mediaQuery`
adaptive breakpoints, the Styles API (`androidx.compose.foundation.style`), `movableContentOf`, and
the experimental LinkBuffer composer runtime path (`ComposeRuntimeFlags.isLinkBufferComposerEnabled`).
These tests run on JVM, iOS, Wasm, and Android instrumented.

## Known Limitations

- **Off-screen lazy items** — `LazyColumn`/`LazyRow` only compose items that are visible. Items that haven't been composed don't exist in the composition tree, so Dejavu has nothing to track. Scroll them into view before asserting.
- **Activity-owned Recomposer clock** — `createAndroidComposeRule` uses the Activity's real `Recomposer`, not a test-controlled one. This means `mainClock.advanceTimeBy()` can't drive infinite animations forward. Use `createComposeRule` (without an Activity) if you need a controllable clock.
- **Parameter change tracking precision** — parameter diffs use `Group.parameters` from the Compose tooling data API, which was designed for Layout Inspector rather than programmatic diffing. Parameter names may be unavailable, and values are compared via `hashCode`/`toString`, so custom types without meaningful `toString` show opaque values.
- **iOS x64** — Compose Multiplatform 1.11 removes Apple x64 target support, so Dejavu supports `iosArm64` and `iosSimulatorArm64` for the 1.11 baseline.
