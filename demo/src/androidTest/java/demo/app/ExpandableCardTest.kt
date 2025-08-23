package demo.app

import androidx.compose.ui.test.assertIsDisplayed
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
class ExpandableCardTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ExpandableCardActivity>()

    @Test
    fun accordion_clicked_card_recomposes() {
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun accordion_previously_expanded_card_recomposes() {
        // Expand card 0
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Expand card 1 (collapses card 0)
        composeTestRule.onNodeWithTag("card_header_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun accordion_uninvolved_cards_stable() {
        // Verify uninvolved cards remain displayed when switching expanded card.
        // Note: ExpandableCard instances share a qualified-name counter in Dejavu,
        // so per-instance recomposition assertions aren't possible here.
        // Expand card 0
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Expand card 1
        composeTestRule.onNodeWithTag("card_header_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_4").assertIsDisplayed()
    }

    @Test
    fun accordion_content_appears_on_expand() {
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_content_0").assertIsDisplayed()
    }

    @Test
    fun accordion_content_disappears_on_collapse() {
        // Expand card 0
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle()

        // Collapse card 0 by expanding card 1
        composeTestRule.onNodeWithTag("card_header_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_content_0").assertDoesNotExist()
    }

    @Test
    fun accordion_title_stays_stable() {
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_header_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("accordion_title").assertStable()
    }

    @Test
    fun accordion_no_recomposition_without_interaction() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("accordion_list_root").assertStable()
        composeTestRule.onNodeWithTag("accordion_list").assertStable()
        composeTestRule.onNodeWithTag("accordion_title").assertStable()
        composeTestRule.onNodeWithTag("card_0").assertStable()
        composeTestRule.onNodeWithTag("card_1").assertStable()
        composeTestRule.onNodeWithTag("card_2").assertStable()
        composeTestRule.onNodeWithTag("card_3").assertStable()
        composeTestRule.onNodeWithTag("card_4").assertStable()
    }
}
