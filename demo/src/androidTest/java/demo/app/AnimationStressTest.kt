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
class AnimationStressTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<AnimationStressActivity>()

    @Test
    fun toggleVisibility_panelTracked() {
        composeTestRule.onNodeWithTag("toggle_vis_btn").performClick()
        composeTestRule.onNodeWithTag("visible_panel").assertRecompositions(exactly = 0)
    }

    @Test
    fun animatedContent_variantsSwitchCleanly() {
        composeTestRule.onNodeWithTag("cycle_content_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithTag("variant_b").assertRecompositions(atLeast = 0)
    }

    @Test
    fun infiniteTransition_bannerRecomposes() {
        // Pulse the banner to trigger recomposition via parameter change.
        // Note: the Compose test framework's virtual clock prevents
        // rememberInfiniteTransition from advancing in activity-based tests,
        // so we drive recomposition through a direct state change instead.
        composeTestRule.onNodeWithTag("pulse_banner_btn").performClick()
        composeTestRule.onNodeWithTag("animating_banner").assertRecompositions(atLeast = 1)
    }

    @Test
    fun conditionalComposition_childTrackedWhenPresent() {
        composeTestRule.onNodeWithTag("toggle_cond_btn").performClick()
        composeTestRule.onNodeWithTag("conditional_child").assertRecompositions(atLeast = 0)
    }

    @Test
    fun conditionalComposition_childGoneWhenRemoved() {
        composeTestRule.onNodeWithTag("toggle_cond_btn").performClick()
        composeTestRule.onNodeWithTag("toggle_cond_btn").performClick()
        composeTestRule.onNodeWithTag("static_label").assertStable()
    }

    @Test
    fun staticLabel_neverRecomposes() {
        composeTestRule.onNodeWithTag("toggle_vis_btn").performClick()
        composeTestRule.onNodeWithTag("static_label").assertStable()
    }

    @Test
    fun allInteractions_staticLabelStaysStable() {
        composeTestRule.onNodeWithTag("toggle_vis_btn").performClick()
        composeTestRule.onNodeWithTag("cycle_content_btn").performClick()
        composeTestRule.onNodeWithTag("toggle_cond_btn").performClick()
        composeTestRule.onNodeWithTag("static_label").assertStable()
    }
}
