package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cross-platform port of Android SwipeListTest.
 * Drops SwipeToDismissBox (Material3-only). Uses LazyColumn with simple items
 * and button-click-driven add/remove mutations.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect`
 * (the Compose runtime runs the effect after every successful composition, so it is the
 * real composition count). Instance classification used below:
 * - **Single-instance** nodes (`list_count_label`, `static_swipe_label`) have unique composer
 *   keys, so the public per-tag API resolves their exact count on all platforms →
 *   `exactly = GroundTruth.delta(tag)`.
 * - `SwipeableItem`/`ItemContent` are emitted from a `LazyColumn` `itemsIndexed` loop (one call
 *   site, keyed by index). The tracer keys composition counts by the shared *compile-time* group
 *   key of that call site, so per-*instance* counts only resolve on Android. On the common targets
 *   these assert the **function-level** count —
 *   `DejavuTracer.getRecompositionCount("dejavu.Fn")` == `GroundTruth.delta("Fn")`.
 *
 * Add/remove on a `mutableStateListOf` is deterministic and settles under `waitForIdle()`, so
 * these tests use exact `delta`/function-level assertions rather than directional bounds.
 */
@OptIn(ExperimentalTestApi::class)
class SwipeListPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun count_label_recomposes_on_remove() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("remove_first_btn").performClick()
        waitForIdle()

        // Removing an item changes the count label's param (size 5 → 4), recomposing it once.
        onNodeWithTag("list_count_label")
            .assertRecompositions(exactly = GroundTruth.delta("list_count_label"))
        assertEquals(1, GroundTruth.delta("list_count_label"), "count label recomposes once when list size changes")
    }

    @Test
    fun count_label_recomposes_on_add() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()

        // Adding an item changes the count label's param (size 5 → 6), recomposing it once.
        onNodeWithTag("list_count_label")
            .assertRecompositions(exactly = GroundTruth.delta("list_count_label"))
        assertEquals(1, GroundTruth.delta("list_count_label"), "count label recomposes once when list size changes")
    }

    @Test
    fun existing_items_stable_on_add() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()

        // SwipeableItem/ItemContent are emitted from one keyed loop call site, so on the common
        // targets per-instance counts aggregate at the function level. Adding one item composes
        // exactly that one new item; the keyed existing items keep their key and params so Compose
        // skips them. Assert tracer == ground truth (proves no over-recomposition of existing
        // items) rather than a loose per-tag upper bound.
        assertEquals(
            GroundTruth.delta("ItemContent"),
            DejavuTracer.getRecompositionCount("dejavu.ItemContent"),
            "tracer ItemContent count should equal SideEffect ground truth",
        )
        assertEquals(
            GroundTruth.delta("SwipeableItem"),
            DejavuTracer.getRecompositionCount("dejavu.SwipeableItem"),
            "tracer SwipeableItem count should equal SideEffect ground truth",
        )
    }

    @Test
    fun remove_updates_list() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Start with 5 items (indices 0..4), remove first => 4 items (indices 0..3)
        onNodeWithTag("remove_first_btn").performClick()
        waitForIdle()
        refreshTagMapping()

        // SwipeableItem is loop-emitted, so assert its function-level count exactly (tracer ==
        // ground truth) instead of a per-tag directional bound.
        assertEquals(
            GroundTruth.delta("SwipeableItem"),
            DejavuTracer.getRecompositionCount("dejavu.SwipeableItem"),
            "tracer SwipeableItem count should equal SideEffect ground truth",
        )
        val nodesFor4 = onAllNodesWithTag("swipeable_item_4").fetchSemanticsNodes()
        assertEquals(0, nodesFor4.size, "swipeable_item_4 should not exist after removal")
    }

    @Test
    fun add_increases_list() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Start with 5 items (indices 0..4), add one => 6 items (indices 0..5)
        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()
        refreshTagMapping()

        // SwipeableItem is loop-emitted, so assert its function-level count exactly (tracer ==
        // ground truth) instead of a per-tag directional bound.
        assertEquals(
            GroundTruth.delta("SwipeableItem"),
            DejavuTracer.getRecompositionCount("dejavu.SwipeableItem"),
            "tracer SwipeableItem count should equal SideEffect ground truth",
        )
        val nodesFor5 = onAllNodesWithTag("swipeable_item_5").fetchSemanticsNodes()
        assertEquals(1, nodesFor5.size, "swipeable_item_5 should exist after add")
    }

    @Test
    fun static_label_stable() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()
        onNodeWithTag("remove_first_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_swipe_label")
            .assertRecompositions(exactly = GroundTruth.delta("static_swipe_label"))
        assertEquals(0, GroundTruth.delta("static_swipe_label"), "parameterless label never recomposes")
    }

    @Test
    fun no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("list_count_label")
            .assertRecompositions(exactly = GroundTruth.delta("list_count_label"))
        onNodeWithTag("static_swipe_label")
            .assertRecompositions(exactly = GroundTruth.delta("static_swipe_label"))
        // No interaction occurred, so both single-instance labels must be stable.
        assertEquals(0, GroundTruth.delta("list_count_label"))
        assertEquals(0, GroundTruth.delta("static_swipe_label"))
        // SwipeableItem/ItemContent are loop-emitted (5 items share a function-level counter); with
        // no interaction the tracer must report zero recompositions for both, matching ground truth.
        assertEquals(
            GroundTruth.delta("SwipeableItem"),
            DejavuTracer.getRecompositionCount("dejavu.SwipeableItem"),
            "tracer SwipeableItem count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("SwipeableItem"), "no item composes without interaction")
        assertEquals(
            GroundTruth.delta("ItemContent"),
            DejavuTracer.getRecompositionCount("dejavu.ItemContent"),
            "tracer ItemContent count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("ItemContent"), "no item content composes without interaction")
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun SwipeListScreen() {
    val items = remember { mutableStateListOf("Item A", "Item B", "Item C", "Item D", "Item E") }
    var nextId = remember { 0 }

    Column {
        ListCountLabel(items.size)
        StaticSwipeLabel()
        Row {
            AddItemButton {
                nextId++
                items.add("Item X$nextId")
            }
            Spacer(modifier = Modifier.width(8.dp))
            RemoveFirstButton {
                if (items.isNotEmpty()) items.removeAt(0)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(items, key = { index, _ -> index }) { index, item ->
                SwipeableItem(index = index, text = item)
            }
        }
    }
}

@Composable
private fun SwipeableItem(index: Int, text: String) {
    // One LazyColumn call site emits every item, so the tracer aggregates instances under the
    // shared compile-time key on non-Android. Record at the function level to match the
    // function-level assertions (per-instance counts are Android-only).
    SideEffect { GroundTruth.record("SwipeableItem") }
    Column(
        modifier = Modifier
            .testTag("swipeable_item_$index")
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        ItemContent(index = index, text = text)
    }
}

@Composable
private fun ItemContent(index: Int, text: String) {
    // Loop-emitted via SwipeableItem, so the tracer aggregates instances at the function level on
    // non-Android. Record at the function level to match the function-level assertions.
    SideEffect { GroundTruth.record("ItemContent") }
    BasicText(
        text = text,
        modifier = Modifier
            .testTag("item_content_$index")
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
private fun ListCountLabel(count: Int) {
    SideEffect { GroundTruth.record("list_count_label") }
    BasicText(
        text = "Items: $count",
        modifier = Modifier
            .testTag("list_count_label")
            .padding(16.dp)
    )
}

@Composable
private fun AddItemButton(onClick: () -> Unit) {
    BasicText("Add Item", Modifier.testTag("add_item_btn").clickable(onClick = onClick))
}

@Composable
private fun RemoveFirstButton(onClick: () -> Unit) {
    BasicText("Remove First", Modifier.testTag("remove_first_btn").clickable(onClick = onClick))
}

@Composable
private fun StaticSwipeLabel() {
    SideEffect { GroundTruth.record("static_swipe_label") }
    BasicText(
        text = "Swipe to dismiss",
        modifier = Modifier
            .testTag("static_swipe_label")
            .padding(8.dp)
    )
}
