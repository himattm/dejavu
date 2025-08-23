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
class PagerCrossfadeTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<PagerCrossfadeActivity>()

    // --- HorizontalPager tests ---

    @Test
    fun pager_initialPageTracked() {
        // Page 0 should be composed initially
        composeTestRule.onNodeWithTag("page_content_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun pager_navigateToNextPage_indicatorRecomposes() {
        composeTestRule.onNodeWithTag("next_page_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithTag("pager_indicator").assertRecompositions(atLeast = 1)
    }

    @Test
    fun pager_navigateToNextPage_siblingStable() {
        composeTestRule.onNodeWithTag("next_page_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithTag("pager_sibling").assertStable()
    }

    // --- Crossfade tests ---

    @Test
    fun crossfade_initialVariantTracked() {
        // Variant A should be composed initially
        composeTestRule.onNodeWithTag("crossfade_a").assertRecompositions(atLeast = 0)
    }

    @Test
    fun crossfade_cycleToVariantB_labelRecomposes() {
        composeTestRule.onNodeWithTag("cycle_crossfade_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithTag("crossfade_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun crossfade_cycleToVariantB_variantBAppears() {
        composeTestRule.onNodeWithTag("cycle_crossfade_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        // Variant B should now be composed
        composeTestRule.onNodeWithTag("crossfade_b").assertRecompositions(atLeast = 0)
    }

    @Test
    fun crossfade_siblingStable() {
        composeTestRule.onNodeWithTag("cycle_crossfade_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithTag("pager_sibling").assertStable()
    }
}
