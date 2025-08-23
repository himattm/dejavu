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
class LazyListStressTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<LazyListStressActivity>()

    @Test
    fun header_neverRecomposes() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.onNodeWithTag("list_header").assertStable()
    }

    @Test
    fun selectOneItem_bannerRecomposesOnce() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.onNodeWithTag("selected_banner").assertRecompositions(exactly = 1)
    }

    @Test
    fun selectOneItem_derivedBannerRecomposesOnce() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.onNodeWithTag("derived_banner").assertRecompositions(exactly = 1)
    }

    @Test
    fun selectSecondItem_derivedBannerStaysStable() {
        // Select first item — hasAnySelected flips false→true
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.waitForIdle()

        // Reset counts so we measure only the next interaction
        composeTestRule.resetRecompositionCounts()

        // Select all — hasAnySelected is still true (was true before, still true after)
        composeTestRule.onNodeWithTag("select_all_btn").performClick()

        // derived_banner should be stable because hasAnySelected didn't change
        composeTestRule.onNodeWithTag("derived_banner").assertStable()
    }

    @Test
    fun selectAll_listItemRecomposes() {
        composeTestRule.onNodeWithTag("select_all_btn").performClick()
        composeTestRule.onNodeWithTag("item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun clearAfterSelectAll_listItemRecomposesAgain() {
        composeTestRule.onNodeWithTag("select_all_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("clear_btn").performClick()
        composeTestRule.onNodeWithTag("item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun tagMapping_lazyItems_resolveCorrectly() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()

        // Verify the tag resolves without error — atLeast = 0 means any count is valid
        composeTestRule.onNodeWithTag("item_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun selectOneItem_listItemRecomposes() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.onNodeWithTag("item_0").assertRecompositions(atLeast = 1)
    }
}
