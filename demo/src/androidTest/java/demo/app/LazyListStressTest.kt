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
        composeTestRule.waitForIdle()
        // The header item is parameterless, so selecting an item must never recompose it.
        composeTestRule.onNodeWithTag("list_header").assertStable()
    }

    @Test
    fun selectOneItem_bannerRecomposesOnce() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.waitForIdle()
        // selectedCount 0 → 1 changes the banner's param, recomposing it exactly once.
        composeTestRule.onNodeWithTag("selected_banner").assertRecompositions(exactly = 1)
    }

    @Test
    fun selectOneItem_derivedBannerRecomposesOnce() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.waitForIdle()
        // hasAnySelected false → true flips the derived banner's param, recomposing it exactly once.
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
        composeTestRule.waitForIdle()
        // Select All sets selectedSet to {0..19}; item_0's `selected` param flips false→true.
        // Android per-instance tracking resolves item_0 individually → exactly one recomposition.
        composeTestRule.onNodeWithTag("item_0").assertRecompositions(exactly = 1)
    }

    @Test
    fun clearAfterSelectAll_listItemRecomposesAgain() {
        composeTestRule.onNodeWithTag("select_all_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("clear_btn").performClick()
        composeTestRule.waitForIdle()
        // Clear empties selectedSet; item_0's `selected` param flips true→false once.
        composeTestRule.onNodeWithTag("item_0").assertRecompositions(exactly = 1)
    }

    @Test
    fun tagMapping_lazyItems_resolveCorrectly() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.waitForIdle()
        // Selecting #0 flips item_0's `selected` param false→true; per-instance tracking resolves
        // the lazy-item tag to exactly that one recomposition (no over- or under-counting).
        composeTestRule.onNodeWithTag("item_0").assertRecompositions(exactly = 1)
    }

    @Test
    fun selectOneItem_listItemRecomposes() {
        composeTestRule.onNodeWithTag("select_0_btn").performClick()
        composeTestRule.waitForIdle()
        // Select #0 toggles item 0 only; its `selected` param flips false→true → exactly one recomposition.
        composeTestRule.onNodeWithTag("item_0").assertRecompositions(exactly = 1)
    }
}
