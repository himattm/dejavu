package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reorderable `LazyColumn` list: swap / shuffle / reset mutations on a `mutableStateListOf`.
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect` (the Compose
 * runtime runs the effect after every successful composition, so it is the real composition count).
 *
 * Instance classification used below:
 * - **Single-instance** nodes (`static_reorder_label`, `list_order_label`) have one call site and a
 *   unique composer key, so the public per-tag API resolves their exact count on every platform →
 *   `exactly = GroundTruth.delta(tag)`.
 * - The `ReorderableItem`s are emitted from a Lazy `items {}` loop — one call site — so the tracer
 *   keys their counts by the shared compile-time group key and per-*instance* counts only resolve on
 *   Android. On the common targets these assert the **function-level** count —
 *   `DejavuTracer.getRecompositionCount("dejavu.ReorderableItem")` == `GroundTruth.delta("ReorderableItem")`.
 *
 * Every test calls [resetRecompositionCounts] + [GroundTruth.snapshotBaseline] after the initial
 * `waitForIdle()` (and again after any mid-test reset). The reset zeroes the Lazy-loop's
 * initial-composition artifact (the six items share one composer key, so instances 2..6 first
 * compose as `totalCount > 1`); snapshotting the ground truth at the same point keeps `delta`/tracer
 * aligned. Reorder mutations settle under `waitForIdle()` before asserting.
 */
@OptIn(ExperimentalTestApi::class)
class ReorderListPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun reorder_swap_affects_first_two_items() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("swap_first_two_btn").performClick()
        waitForIdle()

        // Swapping items[0] and items[1] changes the text emitted at index 0 ("A"→"B") and at
        // index 1 ("B"→"A"); the other four items keep their text and skip. The loop items share one
        // composer key on the common targets, so assert the function-level count: tracer == ground
        // truth, and exactly the two changed items recompose.
        assertEquals(
            GroundTruth.delta("ReorderableItem"),
            DejavuTracer.getRecompositionCount("dejavu.ReorderableItem"),
            "tracer ReorderableItem count should equal SideEffect ground truth",
        )
        assertEquals(2, GroundTruth.delta("ReorderableItem"), "swapping items 0,1 recomposes exactly those two items")
    }

    @Test
    fun reorder_swap_other_items_stable() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("swap_first_two_btn").performClick()
        waitForIdle()

        // Only the first two items change text; the rest are stable. Function-level: the total
        // recompositions across all items is exactly 2, which proves items 2..5 stayed stable (any
        // over-recomposition would push the total above 2). tracer == ground truth.
        assertEquals(
            GroundTruth.delta("ReorderableItem"),
            DejavuTracer.getRecompositionCount("dejavu.ReorderableItem"),
            "tracer ReorderableItem count should equal SideEffect ground truth",
        )
        assertEquals(2, GroundTruth.delta("ReorderableItem"), "only items 0,1 change; items 2..5 stay stable")
    }

    @Test
    fun reorder_shuffle_affects_all_items() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("shuffle_btn").performClick()
        waitForIdle()

        // A shuffle is non-deterministic in WHICH items move (a permutation may even fix some in
        // place), so we can't assert a fixed count. But whatever the runtime composes, Dejavu must
        // report it exactly: tracer == ground truth at the function level.
        assertEquals(
            GroundTruth.delta("ReorderableItem"),
            DejavuTracer.getRecompositionCount("dejavu.ReorderableItem"),
            "tracer ReorderableItem count should equal SideEffect ground truth",
        )
    }

    @Test
    fun reorder_order_label_recomposes_on_mutation() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("swap_first_two_btn").performClick()
        waitForIdle()

        // The swap changes the joined order string, so the label's param changes and it recomposes
        // exactly once.
        onNodeWithTag("list_order_label")
            .assertRecompositions(exactly = GroundTruth.delta("list_order_label"))
        assertEquals(1, GroundTruth.delta("list_order_label"), "order label recomposes once when the list order changes")
    }

    @Test
    fun reorder_reset_restores_order() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()

        onNodeWithTag("shuffle_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("reset_order_btn").performClick()
        waitForIdle()

        // Resetting after a shuffle restores the original order string. The label's param changes
        // (unless the shuffle happened to leave the list in original order — a same-value write that
        // would skip), so assert tracer == ground truth rather than a fixed count.
        onNodeWithTag("list_order_label")
            .assertRecompositions(exactly = GroundTruth.delta("list_order_label"))
    }

    @Test
    fun reorder_static_label_stable() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("swap_first_two_btn").performClick()
        waitForIdle()
        onNodeWithTag("shuffle_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_reorder_label")
            .assertRecompositions(exactly = GroundTruth.delta("static_reorder_label"))
        assertEquals(0, GroundTruth.delta("static_reorder_label"), "parameterless static label never recomposes")
    }

    @Test
    fun reorder_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No interaction occurred, so every node must be stable.
        onNodeWithTag("static_reorder_label")
            .assertRecompositions(exactly = GroundTruth.delta("static_reorder_label"))
        onNodeWithTag("list_order_label")
            .assertRecompositions(exactly = GroundTruth.delta("list_order_label"))
        assertEquals(
            GroundTruth.delta("ReorderableItem"),
            DejavuTracer.getRecompositionCount("dejavu.ReorderableItem"),
            "tracer ReorderableItem count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("static_reorder_label"))
        assertEquals(0, GroundTruth.delta("list_order_label"))
        assertEquals(0, GroundTruth.delta("ReorderableItem"), "no item composes without interaction")
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun ReorderScreen() {
    val items = remember { mutableStateListOf("A", "B", "C", "D", "E", "F") }
    val originalOrder = remember { listOf("A", "B", "C", "D", "E", "F") }
    Column {
        StaticReorderLabel()
        ListOrderLabel(items.joinToString(","))
        LazyColumn {
            items(items.size) { index ->
                ReorderableItem(items[index], index)
            }
        }
        BasicText("Swap", Modifier.testTag("swap_first_two_btn").clickable {
            if (items.size >= 2) { val tmp = items[0]; items[0] = items[1]; items[1] = tmp }
        })
        BasicText("Shuffle", Modifier.testTag("shuffle_btn").clickable { items.shuffle() })
        BasicText("Reset", Modifier.testTag("reset_order_btn").clickable {
            items.clear(); items.addAll(originalOrder)
        })
    }
}

@Composable
private fun StaticReorderLabel() {
    SideEffect { GroundTruth.record("static_reorder_label") }
    BasicText("Reorder List", Modifier.testTag("static_reorder_label"))
}

@Composable
private fun ListOrderLabel(order: String) {
    SideEffect { GroundTruth.record("list_order_label") }
    BasicText("Order: $order", Modifier.testTag("list_order_label"))
}

@Composable
private fun ReorderableItem(text: String, index: Int) {
    // One Lazy `items {}` call site emits every item, so the tracer aggregates them under the shared
    // compile-time key on non-Android. Record at the function level to match the function-level
    // assertions (per-instance counts are Android-only — see demo PerTagTrackingRegressionTest).
    SideEffect { GroundTruth.record("ReorderableItem") }
    BasicText("Item: $text", Modifier.testTag("reorderable_item_$index"))
}
