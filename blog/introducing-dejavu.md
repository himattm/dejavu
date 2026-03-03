# Introducing Dejavu: Recomposition Testing for Jetpack Compose

You spend a week profiling your app. You find the hot path -- a list screen that's recomposing its header on every scroll tick, a product card that re-renders when an unrelated piece of state changes. You restructure the composables, hoist the right state, stabilize the lambdas. The jank vanishes. Scroll performance doubles. You ship it, the team celebrates, and you move on.

Three sprints later, someone files a bug: "the product list feels janky again."

That's the part nobody talks about. Performance work has no protection. There's no compiler error when someone introduces an unstable data class. There's no test failure when a refactor widens a parameter type. There's no CI check when a library bump changes how a lambda captures its closure. The regression is silent, and you find out about it the same way your users do -- by feel.

## What We Built at Square

At Square, the team I worked with ran into this problem at scale. We had dozens of Compose screens, multiple teams contributing to the same codebase, and enough velocity that manual performance audits couldn't keep up. So we built internal tooling to track recomposition counts in tests.

The key insight was simple: make performance a testable contract, not a dashboard you check after the fact. Instead of monitoring recomposition counts in a profiler and hoping nothing changed, we wrote tests that asserted exactly how many times a composable re-rendered for a given interaction. If a PR introduced a recomposition regression, CI caught it. Not a user. Not a bug report. Not a "feels slower" Slack thread three weeks later.

It worked. Regressions that used to slip through for sprints got caught in minutes. I wanted to bring that approach to the broader Compose ecosystem.

## Compose Has the Same Problem

Jetpack Compose's recomposition model is powerful, but it's implicit. The framework decides when to recompose based on state reads, parameter equality, and stability inference. When it works, it's invisible -- composables update exactly when they should and stay stable otherwise. When it breaks, it's also invisible.

The ways it breaks are subtle. An unstable class that the compiler can't prove is immutable. A lambda that gets recreated on every composition because it captures a changing value. A parameter type change that widens equality semantics. None of these produce warnings. None of them fail a build. They just quietly cause your UI to do more work than it needs to.

The existing tools don't solve this. Layout Inspector shows you recomposition counts, but it requires a running app, manual inspection, and can't be automated or run in CI. `SideEffect` counters and `LaunchedEffect` logging are invasive -- they litter your production code with debugging infrastructure that doesn't scale and shouldn't ship. Neither approach gives you what you actually need: a testable, automatable contract that says "this composable should recompose exactly once when this interaction happens" and enforces it on every PR.

## Introducing Dejavu

Dejavu is a test-only library that turns recomposition behavior into assertions. No production code changes beyond the `Modifier.testTag()` calls you probably already have. One-line test setup. Rich diagnostics when something breaks.

Here's what a test looks like:

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

That's it. `createRecompositionTrackingRule` wraps the standard `createAndroidComposeRule`, resets recomposition counts before each test, and exposes assertion extensions on `SemanticsNodeInteraction`. You write tests the same way you already write Compose UI tests -- find a node by tag, assert something about it. The "something" is now recomposition count.

When a test fails, you get structured diagnostics that tell you not just *what* broke, but *why*:

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

Source location, a timeline of when the recomposition happened, which parameters changed, and a causal chain back to the state mutation that triggered it. You don't have to guess. You read the error, find the source, and fix it.

## What Makes It Different

Dejavu hooks into the Compose runtime's `CompositionTracer` API -- no compiler plugin, no bytecode manipulation, no Gradle plugin. It works with the runtime you already ship.

The causality analysis is the part I'm most proud of. Most recomposition tools tell you *that* something recomposed. Dejavu tells you *why*. It tracks `Snapshot` state changes and maps dirty bits back to parameter slots, so you can see the chain from "this `MutableState<Int>` was written to" through "this composable's parameter slot 1 was marked dirty" to "this composable recomposed." That's the difference between knowing you have a problem and knowing how to fix it.

Per-instance tracking means that if you render 50 `ProductItem` composables in a `LazyColumn`, each one gets its own recomposition counter tied to its own `testTag`. You're not asserting against a class -- you're asserting against a specific instance in a specific part of your UI tree.

It's been tested across Compose BOM versions from 1.6.x through 1.10.x. And because it's an `androidTestImplementation` dependency, nothing ships in your production APK. Zero runtime overhead for your users.

## Get Started

Add the dependency:

```kotlin
androidTestImplementation("me.mmckenna.dejavu:dejavu:0.1.1")
```

Read the docs at [dejavu.mmckenna.me](https://dejavu.mmckenna.me). Browse the source and sample apps on [GitHub](https://github.com/himattm/dejavu).

If you've ever shipped a performance fix and then watched it quietly regress, Dejavu is for you. Issues, PRs, and discussions are all welcome -- I'd love to hear what you're building with it.

Lock in the win. Make it permanent.
