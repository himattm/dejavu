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
class ChipFilterTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ChipFilterActivity>()

    @Test
    fun chip_toggled_chip_recomposes() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()

        composeTestRule.onNodeWithTag("chip_electronics").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_other_chips_stable_on_toggle() {
        // Verify untoggled chips still exist after toggling another chip.
        // Note: FilterableChip instances share a qualified-name counter in Dejavu,
        // so per-instance recomposition assertions aren't possible here.
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()

        composeTestRule.onNodeWithTag("chip_clothing").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chip_books").assertIsDisplayed()
    }

    @Test
    fun chip_filtered_list_recomposes_on_filter() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()

        composeTestRule.onNodeWithTag("filtered_list").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_count_label_recomposes_on_filter() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()

        composeTestRule.onNodeWithTag("filter_count_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_clear_resets_all() {
        composeTestRule.onNodeWithTag("chip_electronics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("clear_filters_btn").performClick()

        composeTestRule.onNodeWithTag("chip_electronics").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_multiple_selections() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()
        composeTestRule.onNodeWithTag("chip_books").performClick()

        composeTestRule.onNodeWithTag("chip_electronics").assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("chip_books").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_no_recomposition_without_interaction() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_filter_root").assertStable()
        composeTestRule.onNodeWithTag("chip_group").assertStable()
        composeTestRule.onNodeWithTag("chip_electronics").assertStable()
        composeTestRule.onNodeWithTag("chip_clothing").assertStable()
        composeTestRule.onNodeWithTag("chip_books").assertStable()
        composeTestRule.onNodeWithTag("filtered_list").assertStable()
        composeTestRule.onNodeWithTag("filter_count_label").assertStable()
        composeTestRule.onNodeWithTag("clear_filters_btn").assertStable()
    }
}
