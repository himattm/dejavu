# Dejavu

[![CI](https://github.com/himattm/dejavu/actions/workflows/ci.yml/badge.svg)](https://github.com/himattm/dejavu/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Lock in Compose performance. Catch recomposition regressions before your users do.**

AI coding agents are rewriting UI code, refactoring state management, and restructuring composable hierarchies faster than any team can manually review. A single missed `remember`, a lambda that lost its stability, or a data class that broke structural equality can silently cascade recompositions across your entire screen -- and you won't know until users report jank.

Dejavu is an Android testing library that makes recomposition behavior a **testable contract**. Write assertions that lock in exactly how many times each composable recomposes, and get actionable diagnostics when something changes -- whether that change came from a teammate, a library upgrade, or an AI agent.

- **Zero production code changes** -- composables only need standard `Modifier.testTag()`
- **One-line test setup** -- `createRecompositionTrackingRule<Activity>()` handles everything
- **Rich error diagnostics** -- source location, recomposition timeline, parameter diffs, causality analysis
- **Per-instance tracking** -- multiple instances of the same composable get independent counters
- **Test-focused** -- designed for UI test assertions; consumers control scope via `debugImplementation` or `testImplementation`

### Why test recompositions?

Compose's recomposition behavior is an **implicit contract**. When you write a composable that takes a `count: Int`, you expect it to recompose when `count` changes and stay stable otherwise. But that contract is never written down, and it breaks silently:

```kotlin
// An AI agent "improves" your code by extracting a lambda
val onClick = { viewModel.increment() }  // Was: onClick = viewModel::increment
// Result: ProductCard now recomposes every frame because the lambda is recreated
```

Dejavu turns these implicit contracts into explicit, tested assertions:

```kotlin
composeTestRule.onNodeWithTag("product_card").assertStable()  // Fails if the refactor broke stability
```

## Quick Start

### 1. Add dependency

> **Maven Central publishing is coming soon.** For now, build and publish locally:

```bash
./gradlew :dejavu:publishToMavenLocal
```

Then in your consumer project:

```kotlin
// settings.gradle.kts
repositories {
    mavenLocal()
}

// app/build.gradle.kts
dependencies {
    androidTestImplementation("me.mmckenna.dejavu:dejavu:0.1.0")
    // Or use debugImplementation / testImplementation to exclude from release builds
}
```

### 2. Enable in your Application

```kotlin
// app/src/main/java/com/yourapp/MyApp.kt  (or place in a debug source set to limit to debug builds)
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Dejavu.enable(app = this)
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<manifest>
    <application android:name=".MyApp" />
</manifest>
```

### 3. Write tests

There are two ways to set up Dejavu in your tests:

**Option 1: One-liner with auto-setup (activity-based tests)**

```kotlin
import dejavu.createRecompositionTrackingRule

@get:Rule
val composeTestRule = createRecompositionTrackingRule<MainActivity>()
```

This wraps `createAndroidComposeRule` and automatically enables Dejavu and resets counts before each test.

**Option 2: Standard rule + extensions (works with any rule type)**

```kotlin
import androidx.compose.ui.test.junit4.createComposeRule  // or createAndroidComposeRule<A>()
import dejavu.resetRecompositionCounts

@get:Rule
val composeTestRule = createComposeRule()

@Before
fun setup() = composeTestRule.resetRecompositionCounts()
```

Extension functions work on any `ComposeTestRule`, including `createComposeRule()` and `createAndroidComposeRule<A>()`.

**Example test (works with either approach):**

```kotlin
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import org.junit.Rule
import org.junit.Test

class CounterRecompositionTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<MainActivity>()

    @Test
    fun incrementCounter_onlyValueRecomposes() {
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.waitForIdle()

        // CounterValue receives the changed count -- should recompose once
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = 1)

        // CounterTitle has no dependencies on count -- should stay stable
        composeTestRule.onNodeWithTag("counter_title").assertStable()
    }
}
```

Your composables just need standard test tags:

```kotlin
@Composable
fun CounterTitle() {
    Text("My Counter", modifier = Modifier.testTag("counter_title"))
}

@Composable
fun CounterValue(value: Int) {
    Text("Value: $value", modifier = Modifier.testTag("counter_value"))
}
```

That is it. No custom modifiers, no wrapper composables, no manual tracking calls.

## Run the Demo

The project includes a demo app with 23 screens covering counters, lazy lists, animations, dialogs, subcomposition, and more.

**From the command line** (requires a connected device or running emulator):

```bash
./gradlew :demo:installDebug
```

**From Android Studio:** open the project and run the `demo` module on any device or emulator (API 23+).

## Error Output

When an assertion fails, Dejavu produces a structured diagnostic:

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

Each section tells you something actionable:

- **Composable** -- the qualified name and source file location
- **Expected vs Actual** -- what you asserted versus what happened
- **All tracked composables** -- every composable Dejavu is tracking, with the failure marked
- **Recomposition timeline** -- when each recomposition happened, which parameter slots were dirty, and which parent triggered it
- **Possible cause** -- state change types, same-value writes, and parameter-driven recompositions

## API Reference

### Setup API

```kotlin
import dejavu.Dejavu

// Enable recomposition tracking (call in Application.onCreate or a debug source set)
Dejavu.enable(app = this, logToLogcat = false)

// Disable tracking and clear all data
Dejavu.disable()
```

`logToLogcat = true` prints recomposition events to Logcat for manual debugging. Default is `false` (silent -- diagnostics only appear in test failures).

### Test API

```kotlin
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import dejavu.getRecompositionCount
```

**Test rule (replaces `createAndroidComposeRule`):**

```kotlin
// Option 1: One-liner with auto-setup (activity-based tests)
@get:Rule
val composeTestRule = createRecompositionTrackingRule<MyActivity>()

// Option 2: Standard rule + extensions (works with any rule type)
@get:Rule
val composeTestRule = createComposeRule()  // or createAndroidComposeRule<A>()

@Before
fun setup() = composeTestRule.resetRecompositionCounts()
```

Option 1 auto-enables Dejavu and resets recomposition counts before each test. Option 2 uses extension functions on `ComposeTestRule` for the same functionality.

**Assertions (on any `SemanticsNodeInteraction`):**

```kotlin
// Exact count
composeTestRule.onNodeWithTag("tag").assertRecompositions(exactly = 2)

// Minimum
composeTestRule.onNodeWithTag("tag").assertRecompositions(atLeast = 1)

// Maximum
composeTestRule.onNodeWithTag("tag").assertRecompositions(atMost = 3)

// Range
composeTestRule.onNodeWithTag("tag").assertRecompositions(atLeast = 1, atMost = 5)

// Stability (alias for exactly = 0)
composeTestRule.onNodeWithTag("tag").assertStable()
```

**Mid-test reset:**

```kotlin
// Reset all counts to zero mid-test
composeTestRule.resetRecompositionCounts()
```

**Programmatic count query:**

```kotlin
// Get the current recomposition count for a tag
val count: Int = composeTestRule.getRecompositionCount("tag")
```

### Composable tagging

Use standard Compose test tags. No custom modifiers required:

```kotlin
@Composable
fun MyComponent(value: Int) {
    Text("Value: $value", modifier = Modifier.testTag("my_component"))
}
```

## How It Works

Dejavu hooks into the Compose runtime's `CompositionTracer` API (available since compose-runtime 1.2.0):

1. **Intercepts trace calls** -- `Composer.setTracer()` receives callbacks for every composable enter/exit at runtime
2. **Maps testTag to composable** -- walks the `CompositionData` group tree each frame to find which composable function encloses each `Modifier.testTag()`
3. **Counts recompositions** -- maintains a thread-safe counter per qualified composable name, incrementing on each recomposition (not initial composition)
4. **Tracks causality** -- `Snapshot.registerApplyObserver` detects state changes; dirty bits from the tracer detect parameter-driven recompositions
5. **Reports on assertion failure** -- assembles source location, timeline, tracked composables, and causality into a single structured error message

All tracking runs in the app process on the main thread, making it directly accessible to instrumented tests.

## Compatibility

**Minimum:** compose-runtime 1.2.0 (CompositionTracer API).

| Compose BOM | Compose | Kotlin | Status |
|---|---|---|---|
| 2024.06.00 | 1.6.x | 2.0.x | Tested |
| 2024.09.00 | 1.7.x | 2.0.x | Tested |
| 2025.01.01 | 1.8.x | 2.0.x | Tested |
| 2026.01.01 | 1.10.x | 2.0.x | Baseline |

Dejavu requires Kotlin 2.0+ with the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`).

CI runs a [compatibility matrix](.github/workflows/ci.yml) weekly against each BOM version above.

## Known Limitations

- **Off-screen lazy items** -- items in `LazyColumn`/`LazyRow` that have not been composed (e.g., items scrolled off-screen or not yet visible) cannot be asserted on because they do not exist in the composition.
- **Activity-owned Recomposer clock** -- `mainClock.advanceTimeBy()` does not control the Activity's `MonotonicFrameClock`, so infinite transitions cannot be driven forward in activity-based tests.
- **Parameter change tracking precision** -- parameter diffs rely on `Group.parameters` from the tooling data API. Parameter names may be unavailable for some composables, and values are compared via `hashCode`/`toString`. Custom types without meaningful `toString` show opaque values.

## Further Reading

- [Examples](docs/EXAMPLES.md) -- test patterns for common scenarios
- [Error Messages Guide](docs/ERROR_MESSAGES.md) -- how to read and act on failure output
- [Causality Analysis](docs/WHY_DID_YOU_RENDER.md) -- understanding why composables recompose

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for our community standards.

## License

Apache 2.0
