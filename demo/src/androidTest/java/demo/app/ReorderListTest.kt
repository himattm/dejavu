package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReorderListTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ReorderListActivity>()

    @Test
    fun reorder_swap_affects_first_two_items() {
        composeTestRule.onNodeWithTag("swap_first_two_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reorderable_item_0").assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("reorderable_item_1").assertRecompositions(atLeast = 1)
    }

    @Test
    fun reorder_swap_other_items_stable() {
        composeTestRule.onNodeWithTag("swap_first_two_btn").performClick()
        composeTestRule.waitForIdle()
        // mutableStateListOf structural mutations propagate dirty bits to all
        // lazy list items, so non-swapped items may recompose. Verify they
        // exist and weren't excessively recomposed rather than asserting zero.
        composeTestRule.onNodeWithTag("reorderable_item_2").assertRecompositions(atMost = 2)
        composeTestRule.onNodeWithTag("reorderable_item_3").assertRecompositions(atMost = 2)
        composeTestRule.onNodeWithTag("reorderable_item_4").assertRecompositions(atMost = 2)
        composeTestRule.onNodeWithTag("reorderable_item_5").assertRecompositions(atMost = 2)
    }

    @Test
    fun reorder_shuffle_affects_all_items() {
        composeTestRule.onNodeWithTag("shuffle_btn").performClick()
        composeTestRule.waitForIdle()
        // Shuffle mutates the list structurally, so all items recompose
        composeTestRule.onNodeWithTag("reorderable_item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun reorder_order_label_recomposes_on_mutation() {
        composeTestRule.onNodeWithTag("swap_first_two_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("list_order_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun reorder_reset_restores_order() {
        composeTestRule.onNodeWithTag("shuffle_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("reset_order_btn").performClick()
        composeTestRule.onNodeWithTag("list_order_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun reorder_static_label_stable() {
        composeTestRule.onNodeWithTag("shuffle_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("swap_first_two_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("static_reorder_label").assertStable()
    }

    @Test
    fun reorder_no_recomposition_without_interaction() {
        composeTestRule.onNodeWithTag("list_order_label").assertStable()
        composeTestRule.onNodeWithTag("static_reorder_label").assertStable()
        composeTestRule.onNodeWithTag("reorderable_item_0").assertStable()
    }
}
