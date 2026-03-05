# Getting Started

## 1. Add dependency

```kotlin
// app/build.gradle.kts
dependencies {
    androidTestImplementation("me.mmckenna.dejavu:dejavu:0.1.1")
}
```

## 2. Write a test

```kotlin
@get:Rule
val composeTestRule = createRecompositionTrackingRule<MainActivity>()

@Test
fun incrementCounter_onlyValueRecomposes() {
    composeTestRule.onNodeWithTag("inc_button").performClick()
    composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = 1)
    composeTestRule.onNodeWithTag("counter_title").assertStable() // stable = zero recompositions
}
```

`createRecompositionTrackingRule` wraps `createAndroidComposeRule` and resets counts before each test. For `createComposeRule()` or other rule types, see [Examples](examples.md).

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

See the [Error Messages Guide](error-messages.md) for how to read and act on each section.
