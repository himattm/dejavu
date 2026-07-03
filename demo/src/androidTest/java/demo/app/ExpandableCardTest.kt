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
        // 1: clicking header 0 flips card 0's isExpanded false->true once. ExpandableCard reads only
        // the isExpanded boolean (the animateContentSize() expansion is a layout Modifier, not a
        // composition-read animated value), so the card recomposes exactly once per flip.
        composeTestRule.onNodeWithTag("card_0").assertRecompositions(exactly = 1)
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
        // 1: expanding card 1 makes card 0's isExpanded flip true->false once. ExpandableCard reads
        // only the isExpanded boolean (animateContentSize() is a layout Modifier, not a
        // composition-read value), so the previously-expanded card recomposes exactly once on collapse.
        composeTestRule.onNodeWithTag("card_0").assertRecompositions(exactly = 1)
    }

    @Test
    fun accordion_uninvolved_cards_stable() {
        // Verify uninvolved cards remain displayed and stable when switching expanded card.
        // Android per-instance tracking resolves each card_<n> tag individually.
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
        // Uninvolved cards keep isExpanded == false across the card-0 -> card-1 toggle, so they
        // never recompose. Per-instance Android tracking pins this exactly.
        composeTestRule.onNodeWithTag("card_2").assertStable()
        composeTestRule.onNodeWithTag("card_3").assertStable()
        composeTestRule.onNodeWithTag("card_4").assertStable()
    }

    @Test
    fun accordion_content_appears_on_expand() {
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle() // settle the animateContentSize reveal before asserting
        composeTestRule.onNodeWithTag("card_content_0").assertIsDisplayed()
    }

    @Test
    fun accordion_content_disappears_on_collapse() {
        // Expand card 0
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle()

        // Collapse card 0 by expanding card 1
        composeTestRule.onNodeWithTag("card_header_1").performClick()
        composeTestRule.waitForIdle() // settle the collapse before asserting absence
        composeTestRule.onNodeWithTag("card_content_0").assertDoesNotExist()
    }

    @Test
    fun accordion_title_stays_stable() {
        composeTestRule.onNodeWithTag("card_header_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_header_2").performClick()
        composeTestRule.waitForIdle()
        // The title is parameterless and outside the toggled state, so it never recomposes (0).
        composeTestRule.onNodeWithTag("accordion_title").assertStable()
    }

    @Test
    fun accordion_no_recomposition_without_interaction() {
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
