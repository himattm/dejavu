# Dejavu

*Wait... didn't we just compose this?*

[![CI](https://github.com/himattm/dejavu/actions/workflows/ci.yml/badge.svg)](https://github.com/himattm/dejavu/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/me.mmckenna.dejavu/dejavu)](https://central.sonatype.com/artifact/me.mmckenna.dejavu/dejavu)
[![Compose](https://img.shields.io/badge/Compose-1.6.x–1.10.x-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/develop/ui/compose)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**[Full Documentation](https://dejavu.mmckenna.me)**

**Lock in Compose performance. Catch recomposition regressions before your users do.**

## The Problem

Compose's recomposition behavior is an implicit contract — composables should recompose when their inputs change and stay stable otherwise. But that contract breaks silently, and today's options for catching it are limited:

- **Layout Inspector** — manual, requires a running app, can't automate, can't run in CI
- **Manual tracking code** — `SideEffect` counters, `LaunchedEffect` logging, wrapper composables; invasive, doesn't scale, and ships in your production code
- Neither gives you a **testable, automatable contract** you can enforce on every PR

## What Dejavu Does

Dejavu is a test-only library that turns recomposition behavior into assertions. Tag your composables with standard `Modifier.testTag()`, write expectations against recomposition counts, and get structured diagnostics when something changes — whether from a teammate, a library upgrade, an AI agent rewriting your UI code, or a refactor that silently destabilizes a lambda.

- **Zero production code changes** — just `Modifier.testTag()`
- **One-line test setup** — `createRecompositionTrackingRule<Activity>()`
- **Rich diagnostics** — source location, recomposition timeline, parameter diffs, causality analysis
- **Per-instance tracking** — multiple instances of the same composable get independent counters

## Quick Start

### 1. Add dependency

```kotlin
// app/build.gradle.kts
dependencies {
    androidTestImplementation("me.mmckenna.dejavu:dejavu:0.1.1")
}
```

### 2. Enable in your Application

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Dejavu.enable(app = this)
    }
}
```

### 3. Write a test

```kotlin
@get:Rule
val composeTestRule = createRecompositionTrackingRule<MainActivity>()

@Test
fun incrementCounter_onlyValueRecomposes() {
    composeTestRule.onNodeWithTag("inc_button").performClick()
    composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = 1)
    composeTestRule.onNodeWithTag("counter_title").assertStable()
}
```

`createRecompositionTrackingRule` wraps `createAndroidComposeRule` and resets counts before each test. For `createComposeRule()` or other rule types, see [Examples](https://dejavu.mmckenna.me/examples/).

## What a Failure Looks Like

```
dejavu.UnexpectedRecompositionsError: Recomposition assertion failed for testTag='product_header'
  Composable: demo.app.ui.ProductHeader (ProductList.kt:29)
  Expected: exactly 0 recomposition(s)
  Actual: 1 recomposition(s)

  All tracked composables:
    ProductListScreen = 1
    ProductHeader    = 1  <-- FAILED
    ProductItem      = 1

  Recomposition timeline:
    #1 at +0ms — param slots changed: [1] | parent: ProductListScreen

  Possible cause:
    1 state change(s) of type Int
    Parameter/parent change detected (dirty bits set)
```

See [Error Messages Guide](https://dejavu.mmckenna.me/error-messages/) for how to read and act on each section.

## API Reference

### Assertions

```kotlin
// Exact count
composeTestRule.onNodeWithTag("tag").assertRecompositions(exactly = 2)

// Bounds
composeTestRule.onNodeWithTag("tag").assertRecompositions(atLeast = 1)
composeTestRule.onNodeWithTag("tag").assertRecompositions(atMost = 3)
composeTestRule.onNodeWithTag("tag").assertRecompositions(atLeast = 1, atMost = 5)

// Stability (alias for exactly = 0)
composeTestRule.onNodeWithTag("tag").assertStable()
```

### Utilities

```kotlin
// Reset all counts to zero mid-test
composeTestRule.resetRecompositionCounts()

// Get the current recomposition count for a tag
val count: Int = composeTestRule.getRecompositionCount("tag")

// Enable with Logcat output for manual debugging
Dejavu.enable(app = this, logToLogcat = true)

// Disable tracking and clear all data
Dejavu.disable()
```

## How It Works

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
| 2026.01.01 | 1.10.x | 2.0.x | Baseline |

## Known Limitations

- **Off-screen lazy items** — `LazyColumn`/`LazyRow` only compose items that are visible. Items that haven't been composed don't exist in the composition tree, so Dejavu has nothing to track. Scroll them into view before asserting.
- **Activity-owned Recomposer clock** — `createAndroidComposeRule` uses the Activity's real `Recomposer`, not a test-controlled one. This means `mainClock.advanceTimeBy()` can't drive infinite animations forward. Use `createComposeRule` (without an Activity) if you need a controllable clock.
- **Parameter change tracking precision** — parameter diffs use `Group.parameters` from the Compose tooling data API, which was designed for Layout Inspector rather than programmatic diffing. Parameter names may be unavailable, and values are compared via `hashCode`/`toString`, so custom types without meaningful `toString` show opaque values.

## Further Reading

- [Examples](https://dejavu.mmckenna.me/examples/) — test patterns for common scenarios
- [Error Messages Guide](https://dejavu.mmckenna.me/error-messages/) — how to read and act on failure output
- [Causality Analysis](https://dejavu.mmckenna.me/causality-analysis/) — understanding why composables recompose

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for our community standards.

## License

Apache 2.0
