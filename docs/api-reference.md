# API Reference

## Assertions

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

## Utilities

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
