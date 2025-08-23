# Dejavu: Test Examples

Practical patterns for asserting recomposition behavior in Jetpack Compose UI tests. Every example uses the actual Dejavu API and is based on the real demo screens in this repository.

## Setup

All examples use the same test structure:

```kotlin
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import org.junit.Rule
import org.junit.Test

class MyRecompositionTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<MyActivity>()

    // Tests go here
}
```

Run the demo tests with:

```bash
./gradlew :demo:connectedDebugAndroidTest
```

---

## Example 1: Basic Stability Assertion

**Use case:** Verify a component with no state dependencies does not recompose when sibling state changes.

**Composable (Counter.kt):**

```kotlin
@Composable
fun CounterScreen() {
    var count by remember { mutableIntStateOf(0) }
    Column {
        CounterTitle()
        CounterValue(count)
        IncButton { count++ }
    }
}

@Composable
fun CounterTitle() {
    Text("Dejavu Counter", modifier = Modifier.testTag("counter_title"))
}

@Composable
fun CounterValue(value: Int) {
    Text("Value: $value", modifier = Modifier.testTag("counter_value"))
}
```

**Test:**

```kotlin
@Test
fun incrementCounter_titleStaysStable() {
    composeTestRule.onNodeWithTag("inc_button").performClick()
    composeTestRule.waitForIdle()

    // CounterValue depends on count -- recomposes once
    composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = 1)

    // CounterTitle has no parameters -- stays stable
    composeTestRule.onNodeWithTag("counter_title").assertStable()
}
```

**Expected behavior:** `CounterTitle` takes no parameters, so Compose skips it entirely when `count` changes. `CounterValue` receives the new count as a parameter, triggering exactly one recomposition.

---

## Example 2: Detecting Unnecessary Recompositions (Overly Broad Parameters)

**Use case:** Catch composables that take a broader parameter type than necessary, causing excess recompositions.

**Composable (ProductList.kt):**

```kotlin
// ISSUE: Takes Int when it only needs Boolean
@Composable
fun ProductHeader(selectedCount: Int, tag: String) {
    val badge = if (selectedCount > 0) " ($selectedCount selected)" else ""
    Text(text = "Products$badge", modifier = Modifier.testTag(tag))
}

// FIXED: Takes Boolean -- only recomposes on the flip
@Composable
fun OptimizedProductHeader(hasSelection: Boolean, tag: String) {
    val badge = if (hasSelection) " (items selected)" else ""
    Text(text = "Products$badge", modifier = Modifier.testTag(tag))
}
```

**Test:**

```kotlin
@Test
fun contrast_headerVsOptimizedHeader_onRepeatedSelections() {
    // 5 selections: selectedCount goes 0->1->2->3->4->5
    // Boolean hasSelection flips once: false->true
    repeat(5) {
        composeTestRule.onNodeWithTag("select_button").performClick()
    }

    // ISSUE: ProductHeader recomposes 5 times -- one per Int change
    composeTestRule.onNodeWithTag("product_header").assertRecompositions(atLeast = 5)

    // FIXED: OptimizedProductHeader recomposes once -- the boolean flip
    composeTestRule.onNodeWithTag("optimized_header").assertRecompositions(exactly = 1)
}
```

**Expected behavior:** `ProductHeader` recomposes on every `selectedCount` change (5 times for 5 clicks). `OptimizedProductHeader` only recomposes when `hasSelection` flips from `false` to `true` (once). This 5:1 ratio reveals the wasted work.

---

## Example 3: Unstable Class Parameter (Identity Equality)

**Use case:** Detect recompositions caused by a non-data-class parameter where reference inequality triggers unnecessary work.

**Composable (ProductList.kt):**

```kotlin
// ISSUE: Regular class -- uses Object.equals (reference identity)
class CartSummary(val itemCount: Int, val totalPrice: String)

@Composable
fun CartBanner(summary: CartSummary, tag: String) {
    Text(
        text = "${summary.itemCount} items - ${summary.totalPrice}",
        modifier = Modifier.testTag(tag),
    )
}
```

**Test:**

```kotlin
@Test
fun cartBanner_recomposesOnUnrelatedRefresh() {
    // Refresh changes refreshCount but NOT selectedCount.
    // Parent recomposes and creates a new CartSummary instance.
    // Because CartSummary is not a data class, the new instance != old instance.
    composeTestRule.onNodeWithTag("refresh_button").performClick()

    // CartBanner recomposes despite no logical change to the cart
    composeTestRule.onNodeWithTag("cart_banner").assertRecompositions(atLeast = 1)
}

@Test
fun stableComponent_doesNotRecomposeOnUnrelatedChange() {
    // TotalDisplay depends only on selectedCount, not refreshCount.
    // Compose skips it because the parameter is unchanged.
    composeTestRule.onNodeWithTag("refresh_button").performClick()

    composeTestRule.onNodeWithTag("total_display").assertStable()
}
```

**Expected behavior:** `CartBanner` recomposes because `CartSummary` uses reference equality -- a new instance is always "different" even with identical fields. Making `CartSummary` a `data class` would fix this. Meanwhile, `TotalDisplay` correctly stays stable because its `Int` parameter did not change.

---

## Example 4: LazyColumn Item Tracking

**Use case:** Verify recomposition behavior inside `LazyColumn` subcompositions, including `derivedStateOf` optimization.

**Composable (LazyListStress.kt):**

```kotlin
@Composable
fun LazyListStressScreen() {
    val selectedSet = remember { mutableStateOf(setOf<Int>()) }
    val selectedCount by remember { derivedStateOf { selectedSet.value.size } }
    val hasAnySelected by remember { derivedStateOf { selectedSet.value.isNotEmpty() } }

    Column {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { HeaderItem() }
            item { SelectedCountBanner(selectedCount) }
            item { DerivedBanner(hasAnySelected) }
            items(20, key = { it }) { index ->
                ListItem(index = index, selected = selectedSet.value.contains(index), ...)
            }
        }
        // Control buttons...
    }
}
```

**Test:**

```kotlin
@Test
fun header_neverRecomposes() {
    composeTestRule.onNodeWithTag("select_0_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("list_header").assertStable()
}

@Test
fun selectOneItem_bannerRecomposesOnce() {
    composeTestRule.onNodeWithTag("select_0_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("selected_banner").assertRecompositions(exactly = 1)
}

@Test
fun selectSecondItem_derivedBannerStaysStable() {
    // Select first item -- hasAnySelected flips false->true
    composeTestRule.onNodeWithTag("select_0_btn").performClick()
    composeTestRule.waitForIdle()

    // Reset so we measure only the next interaction
    composeTestRule.resetRecompositionCounts()

    // Select all -- hasAnySelected is STILL true
    composeTestRule.onNodeWithTag("select_all_btn").performClick()
    composeTestRule.waitForIdle()

    // derivedStateOf prevents recomposition because the boolean did not change
    composeTestRule.onNodeWithTag("derived_banner").assertStable()
}
```

**Expected behavior:** Tag mapping works correctly inside `LazyColumn` subcompositions. The `HeaderItem` has no dependencies and stays stable. `derivedStateOf` prevents `DerivedBanner` from recomposing when the derived boolean does not change, even though the underlying set did.

**Limitation:** Off-screen lazy items that have not been composed cannot be asserted on -- they do not exist in the composition tree.

---

## Example 5: Animation Tracking (AnimatedVisibility)

**Use case:** Verify that animated components track correctly and that static siblings remain stable during animations.

**Composable (AnimationStress.kt):**

```kotlin
@Composable
fun AnimationStressScreen() {
    var visible by remember { mutableStateOf(false) }
    Column {
        ToggleVisibilityButton { visible = !visible }
        AnimatedVisibility(visible = visible) {
            VisiblePanel()
        }
        StaticLabel()
    }
}

@Composable
fun VisiblePanel() {
    Text("Visible Panel", modifier = Modifier.testTag("visible_panel"))
}

@Composable
fun StaticLabel() {
    Text("Static Label", modifier = Modifier.testTag("static_label"))
}
```

**Test:**

```kotlin
@Test
fun toggleVisibility_panelTracked() {
    composeTestRule.onNodeWithTag("toggle_vis_btn").performClick()
    composeTestRule.waitForIdle()

    // Initial composition via AnimatedVisibility counts as 0 recompositions
    composeTestRule.onNodeWithTag("visible_panel").assertRecompositions(exactly = 0)
}

@Test
fun staticLabel_neverRecomposes() {
    composeTestRule.onNodeWithTag("toggle_vis_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("static_label").assertStable()
}

@Test
fun allInteractions_staticLabelStaysStable() {
    composeTestRule.onNodeWithTag("toggle_vis_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cycle_content_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("toggle_cond_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("static_label").assertStable()
}
```

**Expected behavior:** When `AnimatedVisibility` first shows `VisiblePanel`, that is initial composition (not a recomposition), so the count is 0. Tag mapping works correctly inside `AnimatedVisibility` subcompositions. Static siblings are completely unaffected by animation state changes.

---

## Example 6: Deep Nesting (6 Levels)

**Use case:** Verify that deeply nested composables all recompose correctly when a shared parameter changes, and that sibling branches remain isolated.

**Composable (DeepNestingStress.kt):**

```kotlin
@Composable
fun DeepNestingStressScreen() {
    var count by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.testTag("deep_root")) {
        Level1Container(count)       // passes count down 6 levels
        SiblingBranch()              // independent subtree
        Button(onClick = { count++ }, modifier = Modifier.testTag("deep_inc_btn")) {
            Text("Increment")
        }
    }
}

@Composable fun Level1Container(count: Int) { /* passes to Level2Panel */ }
@Composable fun Level2Panel(count: Int) { /* passes to Level3Section */ }
// ... through Level6Leaf
@Composable fun SiblingBranch() { /* no count parameter */ }
```

**Test:**

```kotlin
@Test
fun deepNesting_allLevelsRecompose() {
    composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
    composeTestRule.waitForIdle()

    // Every level receives count as a parameter -- all recompose once
    composeTestRule.onNodeWithTag("level_1").assertRecompositions(exactly = 1)
    composeTestRule.onNodeWithTag("level_2").assertRecompositions(exactly = 1)
    composeTestRule.onNodeWithTag("level_3").assertRecompositions(exactly = 1)
    composeTestRule.onNodeWithTag("level_6").assertRecompositions(exactly = 1)
}

@Test
fun deepNesting_siblingBranchIsolated() {
    composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
    composeTestRule.waitForIdle()

    // SiblingBranch has no count parameter -- stays stable
    composeTestRule.onNodeWithTag("sibling_branch").assertStable()
    composeTestRule.onNodeWithTag("sibling_child").assertStable()
}
```

**Expected behavior:** All 6 levels of the nesting chain recompose exactly once because they each receive `count` as a parameter. The sibling branch, which has no dependency on `count`, remains completely stable. This confirms that Compose's invalidation is properly scoped even through deep nesting.

---

## Example 7: Scoped State Invalidation and CompositionLocal

**Use case:** Verify that multiple independent state sources only invalidate the composables that read them, and that CompositionLocal changes are properly scoped.

**Composable (SharedStateStress.kt):**

```kotlin
val LocalThemeMode = compositionLocalOf { false }

@Composable
fun SharedStateStressScreen() {
    var counterA by remember { mutableIntStateOf(0) }
    var counterB by remember { mutableIntStateOf(0) }
    var counterC by remember { mutableIntStateOf(0) }
    var themeMode by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        Column {
            CounterAReader(counterA)
            CounterBReader(counterB)
            CounterCReader(counterC)
            DualReader(counterA, counterB)
            ThemeReader()               // reads LocalThemeMode.current
            AllReader(counterA, counterB, counterC)
            // Buttons to increment each counter and toggle theme...
        }
    }
}
```

**Test:**

```kotlin
@Test
fun incrementA_onlyReaderARecomposes() {
    composeTestRule.onNodeWithTag("inc_a_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("reader_a").assertRecompositions(exactly = 1)
    composeTestRule.onNodeWithTag("reader_b").assertStable()
    composeTestRule.onNodeWithTag("reader_c").assertStable()
}

@Test
fun incrementA_dualReaderRecomposes() {
    composeTestRule.onNodeWithTag("inc_a_btn").performClick()
    composeTestRule.waitForIdle()

    // DualReader depends on both A and B -- recomposes because A changed
    composeTestRule.onNodeWithTag("dual_reader").assertRecompositions(exactly = 1)
}

@Test
fun incrementC_dualReaderStaysStable() {
    composeTestRule.onNodeWithTag("inc_c_btn").performClick()
    composeTestRule.waitForIdle()

    // DualReader only depends on A and B, not C
    composeTestRule.onNodeWithTag("dual_reader").assertStable()
}

@Test
fun toggleTheme_themeReaderRecomposes_countersStable() {
    composeTestRule.onNodeWithTag("toggle_theme_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("theme_reader").assertRecompositions(exactly = 1)
    composeTestRule.onNodeWithTag("reader_a").assertStable()
    composeTestRule.onNodeWithTag("reader_b").assertStable()
    composeTestRule.onNodeWithTag("reader_c").assertStable()
}

@Test
fun incrementAllThree_allReaderRecomposesThreeTimes() {
    composeTestRule.onNodeWithTag("inc_a_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("inc_b_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("inc_c_btn").performClick()
    composeTestRule.waitForIdle()

    // AllReader depends on all three -- recomposes once per change
    composeTestRule.onNodeWithTag("all_reader").assertRecompositions(exactly = 3)
}
```

**Expected behavior:** Each counter reader only recomposes when its specific state changes. `DualReader` recomposes when A or B changes but not C. `ThemeReader` only recomposes on `CompositionLocal` changes. `AllReader` accumulates one recomposition per state source change. This demonstrates Compose's fine-grained invalidation working correctly.

---

## Example 8: Derived State Chain

**Use case:** Verify that `derivedStateOf` prevents recompositions when the derived value does not actually change.

**Composable (LazyListStress.kt):**

```kotlin
val selectedSet = remember { mutableStateOf(setOf<Int>()) }
val hasAnySelected by remember { derivedStateOf { selectedSet.value.isNotEmpty() } }

// DerivedBanner only reads the derived Boolean, not the raw set
DerivedBanner(hasAnySelected)
```

**Test:**

```kotlin
@Test
fun derivedState_preventsRecompositionWhenValueUnchanged() {
    // Select first item -- hasAnySelected flips false->true
    composeTestRule.onNodeWithTag("select_0_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("derived_banner").assertRecompositions(exactly = 1)

    // Reset to measure only the next interaction
    composeTestRule.resetRecompositionCounts()

    // Select all remaining items -- hasAnySelected stays true
    composeTestRule.onNodeWithTag("select_all_btn").performClick()
    composeTestRule.waitForIdle()

    // No recomposition: the derived boolean did not change
    composeTestRule.onNodeWithTag("derived_banner").assertStable()
}
```

**Expected behavior:** `derivedStateOf` computes `selectedSet.value.isNotEmpty()` and only notifies downstream readers when the result changes. Selecting more items changes the set but not the boolean, so `DerivedBanner` stays stable. This is the canonical pattern for coalescing fine-grained state into coarser-grained signals.

---

## Tips

### Use `resetRecompositionCounts()` for multi-phase tests

When a test involves setup followed by the actual interaction under test, reset between phases:

```kotlin
// Setup: get to a known state
composeTestRule.onNodeWithTag("select_0_btn").performClick()
composeTestRule.waitForIdle()

// Reset so counts reflect only the next interaction
composeTestRule.resetRecompositionCounts()

// Actual test: this is the interaction we care about
composeTestRule.onNodeWithTag("select_all_btn").performClick()
composeTestRule.waitForIdle()

composeTestRule.onNodeWithTag("derived_banner").assertStable()
```

### Use `getRecompositionCount()` for programmatic checks

```kotlin
val count = composeTestRule.getRecompositionCount("my_tag")
assertTrue(count < performanceBudget, "Exceeded budget: $count > $performanceBudget")
```

### Prefer `assertStable()` for components that should never recompose

`assertStable()` is clearer than `assertRecompositions(exactly = 0)` and communicates intent:

```kotlin
// Clear intent: this component should be stable
composeTestRule.onNodeWithTag("header").assertStable()
```

### Use `atMost` for performance budgets

When you do not know the exact count but want to bound it:

```kotlin
// CartBanner recomposes on every parent recomposition (known issue).
// Budget: no more than 1:1 with parent recompositions.
composeTestRule.onNodeWithTag("cart_banner").assertRecompositions(atMost = 5)
```

### Use consistent tag naming

Define tag constants to avoid typos between composables and tests:

```kotlin
object Tags {
    const val PRODUCT_HEADER = "product_header"
    const val COUNTER_VALUE = "counter_value"
}

// In composable
Text("Header", modifier = Modifier.testTag(Tags.PRODUCT_HEADER))

// In test
composeTestRule.onNodeWithTag(Tags.PRODUCT_HEADER).assertStable()
```
