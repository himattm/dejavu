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
class StarRatingTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<StarRatingActivity>()

    @Test
    fun rating_changed_stars_recompose_unchanged_stable() {
        // Rating 0 → 3: stars 0-2 change isFilled (false→true), stars 3-4 stay false
        composeTestRule.onNodeWithTag("set_rating_3_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: isFilled flips false→true once for each of the lower three stars.
        composeTestRule.onNodeWithTag("star_0").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("star_1").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("star_2").assertRecompositions(exactly = 1)
        // 0: isFilled stays false; per-instance tracking reports these as stable.
        composeTestRule.onNodeWithTag("star_3").assertStable()
        composeTestRule.onNodeWithTag("star_4").assertStable()
    }

    @Test
    fun rating_display_recomposes_on_change() {
        composeTestRule.onNodeWithTag("set_rating_3_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: rating changes 0f→3f once; the display reads rating, so it recomposes once.
        composeTestRule.onNodeWithTag("rating_display").assertRecompositions(exactly = 1)
    }

    @Test
    fun rating_same_value_no_recomposition() {
        composeTestRule.onNodeWithTag("set_rating_3_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("set_rating_3_btn").performClick()
        composeTestRule.onNodeWithTag("star_0").assertStable()
        composeTestRule.onNodeWithTag("star_1").assertStable()
        composeTestRule.onNodeWithTag("star_2").assertStable()
        composeTestRule.onNodeWithTag("star_3").assertStable()
        composeTestRule.onNodeWithTag("star_4").assertStable()
    }

    @Test
    fun rating_static_label_stable() {
        composeTestRule.onNodeWithTag("set_rating_1_btn").performClick()
        composeTestRule.onNodeWithTag("set_rating_3_btn").performClick()
        composeTestRule.onNodeWithTag("set_rating_5_btn").performClick()
        composeTestRule.onNodeWithTag("static_rating_label").assertStable()
    }

    @Test
    fun rating_buttons_stable() {
        composeTestRule.onNodeWithTag("set_rating_3_btn").performClick()
        composeTestRule.onNodeWithTag("set_rating_1_btn").assertStable()
        composeTestRule.onNodeWithTag("set_rating_5_btn").assertStable()
    }

    @Test
    fun rating_sequential_changes() {
        // 0→1 flips star_0 (false→true); 1→5 flips stars 1-4 (false→true).
        composeTestRule.onNodeWithTag("set_rating_1_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("set_rating_5_btn").performClick()
        composeTestRule.waitForIdle()
        // 1 each: every star's isFilled flips exactly once across the two rating changes
        // (star_0 on 0→1; stars 1-4 on 1→5).
        composeTestRule.onNodeWithTag("star_0").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("star_1").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("star_2").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("star_3").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("star_4").assertRecompositions(exactly = 1)
    }

    @Test
    fun rating_no_recomposition_without_interaction() {
        composeTestRule.onNodeWithTag("rating_bar_root").assertStable()
        composeTestRule.onNodeWithTag("rating_bar").assertStable()
        composeTestRule.onNodeWithTag("star_0").assertStable()
        composeTestRule.onNodeWithTag("star_1").assertStable()
        composeTestRule.onNodeWithTag("star_2").assertStable()
        composeTestRule.onNodeWithTag("star_3").assertStable()
        composeTestRule.onNodeWithTag("star_4").assertStable()
        composeTestRule.onNodeWithTag("rating_display").assertStable()
        composeTestRule.onNodeWithTag("static_rating_label").assertStable()
    }
}
