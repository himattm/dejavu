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
class CollapsingHeaderTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<CollapsingHeaderActivity>()

    @Test
    fun header_recomposes_on_scroll() {
        composeTestRule.onNodeWithTag("scroll_to_bottom_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("collapsing_header").assertRecompositions(atLeast = 1)
    }

    @Test
    fun header_image_recomposes_on_scroll() {
        composeTestRule.onNodeWithTag("scroll_to_bottom_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("header_image").assertRecompositions(atLeast = 1)
    }

    @Test
    fun header_title_recomposes_on_scroll() {
        composeTestRule.onNodeWithTag("scroll_to_bottom_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("header_title").assertRecompositions(atLeast = 1)
    }

    @Test
    fun scroll_content_items_stable_during_scroll() {
        composeTestRule.onNodeWithTag("scroll_to_bottom_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scroll_content_item_0").assertStable()
    }

    @Test
    fun scroll_to_top_restores_header() {
        composeTestRule.onNodeWithTag("scroll_to_bottom_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("scroll_to_top_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("collapsing_header").assertRecompositions(atLeast = 1)
    }

    @Test
    fun header_no_recomposition_without_interaction() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("collapsing_header_root").assertStable()
        composeTestRule.onNodeWithTag("collapsing_header").assertStable()
        composeTestRule.onNodeWithTag("header_image").assertStable()
        composeTestRule.onNodeWithTag("header_title").assertStable()
        composeTestRule.onNodeWithTag("scroll_content_item_0").assertStable()
    }
}
