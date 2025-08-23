package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipeListTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<SwipeListActivity>()

    @Test
    fun swipe_count_label_recomposes_on_remove() {
        composeTestRule.onNodeWithTag("remove_first_btn").performClick()
        composeTestRule.onNodeWithTag("list_count_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun swipe_count_label_recomposes_on_add() {
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        composeTestRule.onNodeWithTag("list_count_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun swipe_existing_items_stable_on_add() {
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        // mutableStateListOf mutations invalidate all readers, so existing items
        // may recompose even though their data didn't change. Allow up to 1.
        composeTestRule.onNodeWithTag("item_content_0").assertRecompositions(atMost = 1)
    }

    @Test
    fun swipe_remove_updates_list() {
        // Start with 5 items (indices 0..4), remove first => 4 items (indices 0..3)
        composeTestRule.onNodeWithTag("remove_first_btn").performClick()
        composeTestRule.onNodeWithTag("swipeable_item_3").assertExists()
        composeTestRule.onNodeWithTag("swipeable_item_4").assertDoesNotExist()
    }

    @Test
    fun swipe_add_increases_list() {
        // Start with 5 items (indices 0..4), add one => 6 items (indices 0..5)
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        composeTestRule.onNodeWithTag("swipeable_item_5").assertExists()
    }

    @Test
    fun swipe_static_label_stable() {
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        composeTestRule.onNodeWithTag("remove_first_btn").performClick()
        composeTestRule.onNodeWithTag("static_swipe_label").assertStable()
    }

    @Test
    fun swipe_no_recomposition_without_interaction() {
        composeTestRule.onNodeWithTag("list_count_label").assertStable()
        composeTestRule.onNodeWithTag("static_swipe_label").assertStable()
        composeTestRule.onNodeWithTag("swipeable_item_0").assertStable()
    }
}
