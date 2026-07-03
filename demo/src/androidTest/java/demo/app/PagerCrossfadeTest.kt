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

/**
 * Android instrumented counterpart of [dejavu.PagerCrossfadePatternTest]. Because per-tag tracking is
 * complete on Android (per-instance, including each HorizontalPager page), every assertion below is
 * pinned exact. Pager flings and Crossfade transitions are finite, so each test settles the
 * interaction with [waitForIdle] before asserting; `resetRecompositionCounts()` drops the initial
 * composition tally so counts measure only post-interaction recompositions.
 *
 * First-composition subtlety (Crossfade variant B): Dejavu counts a key's first composition as the
 * initial composition, not a recomposition. A variant that only appears after the reset therefore
 * reports 0 recompositions even though it really did compose once — see
 * [crossfade_cycleToVariantB_variantBAppears].
 */
@RunWith(AndroidJUnit4::class)
class PagerCrossfadeTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<PagerCrossfadeActivity>()

    // --- HorizontalPager tests ---

    @Test
    fun pager_initialPageTracked() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()
        // Page 0 composes once at init; that initial composition is dropped by the reset and no
        // interaction follows, so per-instance tracking reports it stable.
        composeTestRule.onNodeWithTag("page_content_0").assertStable()
    }

    @Test
    fun pager_navigateToNextPage_indicatorRecomposes() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("next_page_btn").performClick()
        // Settle the finite fling so currentPage lands on 1 before asserting.
        composeTestRule.waitForIdle()
        // 1: animateScrollToPage(1) settles currentPage 0→1, a single param change; the indicator
        // reads currentPage, so it recomposes exactly once.
        composeTestRule.onNodeWithTag("pager_indicator").assertRecompositions(exactly = 1)
    }

    @Test
    fun pager_navigateToNextPage_siblingStable() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("next_page_btn").performClick()
        composeTestRule.waitForIdle()
        // The parameterless sibling is unaffected by the pager fling.
        composeTestRule.onNodeWithTag("pager_sibling").assertStable()
    }

    // --- Crossfade tests ---

    @Test
    fun crossfade_initialVariantTracked() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()
        // Variant A composes once at init (dropped by the reset); no interaction → no recomposition.
        composeTestRule.onNodeWithTag("crossfade_a").assertStable()
    }

    @Test
    fun crossfade_cycleToVariantB_labelRecomposes() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("cycle_crossfade_btn").performClick()
        // Let the finite crossfade transition settle before asserting.
        composeTestRule.waitForIdle()
        // 1: crossfadeTarget 0→1 is one param change; the label reads target, so it recomposes once.
        composeTestRule.onNodeWithTag("crossfade_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun crossfade_cycleToVariantB_variantBAppears() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("cycle_crossfade_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: on Android the real Crossfade keeps both variants mounted through the fade and updates
        // its visible-target state, which recomposes the newly-shown variant B once after its initial
        // composition. (In the windowless/animationless common test this settles at 0 — see
        // PagerCrossfadePatternTest.)
        composeTestRule.onNodeWithTag("crossfade_b").assertRecompositions(exactly = 1)
    }

    @Test
    fun crossfade_siblingStable() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("cycle_crossfade_btn").performClick()
        composeTestRule.waitForIdle()
        // The parameterless sibling is unaffected by the crossfade transition.
        composeTestRule.onNodeWithTag("pager_sibling").assertStable()
    }
}
