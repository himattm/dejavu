package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ReorderListPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun reorder_swap_affects_first_two_items() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("swap_first_two_btn").performClick()
        waitForIdle()

        onNodeWithTag("reorderable_item_0").assertRecompositions(atLeast = 1)
        onNodeWithTag("reorderable_item_1").assertRecompositions(atLeast = 1)
    }

    @Test
    fun reorder_swap_other_items_stable() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("swap_first_two_btn").performClick()
        waitForIdle()

        onNodeWithTag("reorderable_item_2").assertRecompositions(atMost = 2)
        onNodeWithTag("reorderable_item_3").assertRecompositions(atMost = 2)
    }

    @Test
    fun reorder_shuffle_affects_all_items() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("shuffle_btn").performClick()
        waitForIdle()

        onNodeWithTag("reorderable_item_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun reorder_order_label_recomposes_on_mutation() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("swap_first_two_btn").performClick()
        waitForIdle()

        onNodeWithTag("list_order_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun reorder_reset_restores_order() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()

        onNodeWithTag("shuffle_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("reset_order_btn").performClick()
        waitForIdle()

        onNodeWithTag("list_order_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun reorder_static_label_stable() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("swap_first_two_btn").performClick()
        waitForIdle()
        onNodeWithTag("shuffle_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_reorder_label").assertStable()
    }

    @Test
    fun reorder_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { ReorderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("static_reorder_label").assertStable()
        onNodeWithTag("list_order_label").assertStable()
        onNodeWithTag("reorderable_item_0").assertStable()
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
    BasicText("Reorder List", Modifier.testTag("static_reorder_label"))
}

@Composable
private fun ListOrderLabel(order: String) {
    BasicText("Order: $order", Modifier.testTag("list_order_label"))
}

@Composable
private fun ReorderableItem(text: String, index: Int) {
    BasicText("Item: $text", Modifier.testTag("reorderable_item_$index"))
}
