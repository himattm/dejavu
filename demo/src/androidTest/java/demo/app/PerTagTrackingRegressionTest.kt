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
 * Regression tests for per-tag recomposition tracking fixes.
 *
 * These tests guard against three interrelated bugs that caused incorrect
 * recomposition counts for multi-instance and multi-tag composables:
 *
 * 1. **Identity-based multi-instance detection**: A single composable with
 *    multiple testTags (e.g., root + button) was incorrectly classified as
 *    multi-instance because tag count > 1. Fix: use Group identity (Anchor)
 *    to distinguish actual separate instances from multiple tags on one instance.
 *
 * 2. **Null-fallback overcounting**: When per-tag tracking had no count entry
 *    for a stable tag, it returned null, causing fallback to the shared
 *    function-level count. Fix: track which tags have established a fingerprint
 *    baseline and return 0 (stable) instead of null for those tags.
 *
 * 3. **Runtime internal fingerprint drift**: `androidx.compose.runtime.*`
 *    objects in `group.data` (e.g., RecomposeScopeImpl) use identity-based
 *    hashCode that drifts between buildTagMapping passes without actual
 *    recomposition. Fix: filter these objects from the fingerprint computation.
 */

/**
 * Guards fix 1: single-instance composable with multiple tags must NOT be
 * treated as multi-instance.
 */
@RunWith(AndroidJUnit4::class)
class PerTagTrackingRegressionTest_Identity {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<DeepNestingStressActivity>()

    /**
     * DeepNestingStressActivity has `deep_root` and `deep_inc_btn` tags that both
     * map to the same composable function. They share one Group identity, so
     * isMultiInstanceFunction should return false and per-tag tracking should
     * use the direct per-tag path (not the multi-instance fallback).
     */
    @Test
    fun singleInstanceWithMultipleTags_usesCorrectTrackingPath() {
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()

        // Both tags belong to the same composable instance — the root tag should
        // see the recomposition via per-tag tracking, not be inflated by shared counts
        composeTestRule.onNodeWithTag("deep_root").assertRecompositions(atLeast = 1)
    }
}

/**
 * Guards fix 2: unchanged multi-instance composables report 0 recompositions
 * instead of falling back to the shared function-level count.
 */
@RunWith(AndroidJUnit4::class)
class PerTagTrackingRegressionTest_NullFallback {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<StarRatingActivity>()

    /**
     * Rating 0→3: star_0 through star_2 change (isFilled false→true), but
     * star_3 and star_4 stay unchanged (isFilled stays false). With the fix,
     * star_3 returns 0 from per-tag tracking instead of null, preventing
     * fallback to Star's shared function-level count.
     */
    @Test
    fun multiInstance_unchangedInstancesReportZero() {
        composeTestRule.onNodeWithTag("set_rating_3_btn").performClick()

        // Stars that changed should show recomposition
        composeTestRule.onNodeWithTag("star_0").assertRecompositions(atLeast = 1)

        // Stars that didn't change should be stable (not inherit shared count)
        composeTestRule.onNodeWithTag("star_3").assertStable()
        composeTestRule.onNodeWithTag("star_4").assertStable()
    }

    /**
     * Guards fix 2 across a reset boundary: after reset, the tagsWithFingerprint
     * set is cleared and must be re-established. Unchanged instances must still
     * be reported as stable after the new baseline is established.
     *
     * Rating 0→3, reset, then 3→5: stars 0-2 stay filled (stable), stars 3-4
     * change from unfilled to filled (recompose).
     */
    @Test
    fun perTagStability_afterReset_unchangedInstancesStillStable() {
        // Establish initial state: rating = 3
        composeTestRule.onNodeWithTag("set_rating_3_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Change rating 3→5: stars 0-2 stay filled, stars 3-4 become filled
        composeTestRule.onNodeWithTag("set_rating_5_btn").performClick()

        // Stars 0-2 didn't change — should be stable even after reset
        composeTestRule.onNodeWithTag("star_0").assertStable()
        composeTestRule.onNodeWithTag("star_1").assertStable()
        composeTestRule.onNodeWithTag("star_2").assertStable()

        // Stars 3-4 changed — should show recomposition
        composeTestRule.onNodeWithTag("star_3").assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("star_4").assertRecompositions(atLeast = 1)
    }
}

/**
 * Guards fix 3: runtime internal objects in group.data must not cause
 * false-positive fingerprint changes for existing loop items.
 */
@RunWith(AndroidJUnit4::class)
class PerTagTrackingRegressionTest_RuntimeInternals {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<KeyIdentityActivity>()

    /**
     * Adding a new LoopItem triggers the parent to recompose, which causes
     * buildTagMapping to re-walk the tree. Without filtering runtime internals,
     * RecomposeScopeImpl objects in group.data would produce different hashCodes
     * between passes, making existing items appear to have recomposed.
     */
    @Test
    fun loopItems_noFalsePositiveFromRuntimeInternals() {
        composeTestRule.onNodeWithTag("add_loop_btn").performClick()

        // Existing items should be stable — their parameters haven't changed
        composeTestRule.onNodeWithTag("loop_item_0").assertStable()
        composeTestRule.onNodeWithTag("loop_item_1").assertStable()
        composeTestRule.onNodeWithTag("loop_item_2").assertStable()

        // The count label should recompose (item count changed)
        composeTestRule.onNodeWithTag("loop_count_label").assertRecompositions(exactly = 1)
    }
}
