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
        // Start with 5 items; remove first => 4. list_count_label reads items.size, so its
        // Int param changes 5 → 4 exactly once. It is single-instance (one call site, unique
        // key), so per-tag tracking resolves its count exactly on every platform.
        composeTestRule.onNodeWithTag("remove_first_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: items.size changes 5 → 4 once, recomposing the count label once.
        composeTestRule.onNodeWithTag("list_count_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun swipe_count_label_recomposes_on_add() {
        // Start with 5 items; add one => 6. list_count_label's Int param changes 5 → 6 once.
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: items.size changes 5 → 6 once, recomposing the count label once.
        composeTestRule.onNodeWithTag("list_count_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun swipe_existing_items_stable_on_add() {
        // The LazyColumn is keyed by index (key = { index, _ -> index }) and Add appends the new
        // item at the end (index 5). Existing item_content_0 keeps its key (0) and its params
        // (index = 0, text = "Item A"), so Compose skips it. Per-instance per-tag tracking
        // (Android, Compose floor 1.10) resolves item_content_0's own count, which is 0.
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        composeTestRule.waitForIdle()
        // 0: append-at-end leaves item 0's key and params unchanged, so it does not recompose.
        composeTestRule.onNodeWithTag("item_content_0").assertStable()
    }

    @Test
    fun swipe_remove_updates_list() {
        // Start with 5 items (indices 0..4), remove first => 4 items (indices 0..3)
        composeTestRule.onNodeWithTag("remove_first_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("swipeable_item_3").assertExists()
        composeTestRule.onNodeWithTag("swipeable_item_4").assertDoesNotExist()
    }

    @Test
    fun swipe_add_increases_list() {
        // Start with 5 items (indices 0..4), add one => 6 items (indices 0..5)
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("swipeable_item_5").assertExists()
    }

    @Test
    fun swipe_static_label_stable() {
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("remove_first_btn").performClick()
        composeTestRule.waitForIdle()
        // static_swipe_label is parameterless, so list mutations never invalidate it.
        composeTestRule.onNodeWithTag("static_swipe_label").assertStable()
    }

    @Test
    fun swipe_no_recomposition_without_interaction() {
        composeTestRule.onNodeWithTag("list_count_label").assertStable()
        composeTestRule.onNodeWithTag("static_swipe_label").assertStable()
        composeTestRule.onNodeWithTag("swipeable_item_0").assertStable()
    }
}
