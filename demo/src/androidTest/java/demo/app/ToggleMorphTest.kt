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
class ToggleMorphTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ToggleMorphActivity>()

    @Test
    fun toggle_animated_composables_recompose_during_animation() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("toggle_btn").performClick()
        composeTestRule.mainClock.advanceTimeBy(200)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_thumb").assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("toggle_track").assertRecompositions(atLeast = 1)
    }

    @Test
    fun toggle_label_recomposes_once_on_toggle() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("toggle_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun toggle_static_sibling_stays_stable() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("toggle_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("static_morph_sibling").assertStable()
    }

    @Test
    fun toggle_button_stays_stable() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("toggle_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_btn").assertStable()
    }

    @Test
    fun toggle_all_animated_parts_recompose() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("toggle_btn").performClick()
        composeTestRule.mainClock.advanceTimeBy(200)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_thumb").assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("toggle_track").assertRecompositions(atLeast = 1)
    }

    @Test
    fun toggle_no_recomposition_without_interaction() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_morph_root").assertStable()
        composeTestRule.onNodeWithTag("morph_toggle").assertStable()
        composeTestRule.onNodeWithTag("toggle_thumb").assertStable()
        composeTestRule.onNodeWithTag("toggle_track").assertStable()
        composeTestRule.onNodeWithTag("toggle_label").assertStable()
        composeTestRule.onNodeWithTag("toggle_btn").assertStable()
        composeTestRule.onNodeWithTag("static_morph_sibling").assertStable()
    }
}
