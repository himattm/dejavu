# How It Works

Dejavu hooks into the Compose runtime's `CompositionTracer` API (available since compose-runtime 1.2.0):

1. **Intercepts trace calls** — `Composer.setTracer()` receives callbacks for every composable enter/exit
2. **Maps testTag to composable** — walks the `CompositionData` group tree to find which composable encloses each `Modifier.testTag()`
3. **Counts recompositions** — maintains a thread-safe counter per composable, incrementing on recomposition (not initial composition)
4. **Tracks causality** — `Snapshot.registerApplyObserver` detects state changes; dirty bits detect parameter-driven recompositions
5. **Reports on failure** — assembles source location, timeline, tracked composables, and causality into a structured error

All tracking runs in the app process on the main thread, directly accessible to instrumented tests.

## Compatibility

**Minimum:** compose-runtime 1.2.0 (CompositionTracer API). Requires Kotlin 2.0+ with the Compose compiler plugin.

| Compose BOM | Compose | Kotlin | Status |
|---|---|---|---|
| 2024.06.00 | 1.6.x | 2.0.x | Tested |
| 2024.09.00 | 1.7.x | 2.0.x | Tested |
| 2025.01.01 | 1.8.x | 2.0.x | Tested |
| 2026.01.01 | 1.10.x | 2.1.x+ | Tested |
| 2026.03.01 | 1.10.x | 2.1.x+ | Baseline |

## Known Limitations

- **Off-screen lazy items** — `LazyColumn`/`LazyRow` only compose items that are visible. Items that haven't been composed don't exist in the composition tree, so Dejavu has nothing to track. Scroll them into view before asserting.
- **Activity-owned Recomposer clock** — `createAndroidComposeRule` uses the Activity's real `Recomposer`, not a test-controlled one. This means `mainClock.advanceTimeBy()` can't drive infinite animations forward. Use `createComposeRule` (without an Activity) if you need a controllable clock.
- **Parameter change tracking precision** — parameter diffs use `Group.parameters` from the Compose tooling data API, which was designed for Layout Inspector rather than programmatic diffing. Parameter names may be unavailable, and values are compared via `hashCode`/`toString`, so custom types without meaningful `toString` show opaque values.
