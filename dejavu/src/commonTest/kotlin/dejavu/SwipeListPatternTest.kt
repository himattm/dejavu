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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cross-platform port of Android SwipeListTest.
 * Drops SwipeToDismissBox (Material3-only). Uses LazyColumn with simple items
 * and button-click-driven add/remove mutations.
 */
@OptIn(ExperimentalTestApi::class)
class SwipeListPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun count_label_recomposes_on_remove() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("remove_first_btn").performClick()
        waitForIdle()

        onNodeWithTag("list_count_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun count_label_recomposes_on_add() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()

        onNodeWithTag("list_count_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun existing_items_stable_on_add() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()

        // mutableStateListOf mutations invalidate all readers, so existing items
        // may recompose even though their data didn't change. Allow up to 1.
        onNodeWithTag("item_content_0").assertRecompositions(atMost = 1)
    }

    @Test
    fun remove_updates_list() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()

        // Start with 5 items (indices 0..4), remove first => 4 items (indices 0..3)
        onNodeWithTag("remove_first_btn").performClick()
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("swipeable_item_3").assertRecompositions(atLeast = 0)
        val nodesFor4 = onAllNodesWithTag("swipeable_item_4").fetchSemanticsNodes()
        assertEquals(0, nodesFor4.size, "swipeable_item_4 should not exist after removal")
    }

    @Test
    fun add_increases_list() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()

        // Start with 5 items (indices 0..4), add one => 6 items (indices 0..5)
        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("swipeable_item_5").assertRecompositions(atLeast = 0)
    }

    @Test
    fun static_label_stable() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()
        onNodeWithTag("remove_first_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_swipe_label").assertStable()
    }

    @Test
    fun no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { SwipeListScreen() } }
        waitForIdle()

        onNodeWithTag("list_count_label").assertStable()
        onNodeWithTag("static_swipe_label").assertStable()
        // SwipeableItem/ItemContent are multi-instance (5 items share counter).
        // Per-instance stability is tested via resetRecompositionCounts in
        // existing_items_stable_on_add.
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
    BasicText(
        text = "Swipe to dismiss",
        modifier = Modifier
            .testTag("static_swipe_label")
            .padding(8.dp)
    )
}
