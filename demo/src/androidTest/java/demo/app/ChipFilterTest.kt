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

/**
 * Android instrumented mirror of the cross-platform [dejavu.ChipFilterPatternTest].
 *
 * On Android, Dejavu per-tag tracking is COMPLETE: the keyless `FilterableChip` loop instances
 * each resolve to their own per-instance recomposition count (Choreographer fingerprinting), so
 * every assertion here is pinned to an EXACT count rather than the function-level sum used on the
 * common targets. The `FilterChip` internal `animateColorAsState` produces a flood of recompositions
 * inside Material3's `AnimatingChipContent` lambda, but those land on the chip's anonymous content
 * lambda — NOT the tagged `FilterableChip` node — so they do not affect these per-tag assertions.
 */
@RunWith(AndroidJUnit4::class)
class ChipFilterTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ChipFilterActivity>()

    @Test
    fun chip_toggled_chip_recomposes() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()
        composeTestRule.waitForIdle()

        // 1: toggling electronics flips its own isSelected; the chip recomposes exactly once.
        composeTestRule.onNodeWithTag("chip_electronics").assertRecompositions(exactly = 1)
    }

    @Test
    fun chip_other_chips_stable_on_toggle() {
        // On Android, FilterableChip instances resolve per-instance, so we can assert that toggling
        // one chip leaves the others stable (the parent's onToggle lambda is remember-ed, so the
        // unselected chips receive identical params and skip).
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("chip_clothing").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chip_books").assertIsDisplayed()
        // 0: only electronics changed; clothing and books read no changed state and stay stable.
        composeTestRule.onNodeWithTag("chip_clothing").assertStable()
        composeTestRule.onNodeWithTag("chip_books").assertStable()
    }

    @Test
    fun chip_filtered_list_recomposes_on_filter() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()
        composeTestRule.waitForIdle()

        // 1: one filter change → filteredProducts changes once → the list recomposes exactly once.
        composeTestRule.onNodeWithTag("filtered_list").assertRecompositions(exactly = 1)
    }

    @Test
    fun chip_count_label_recomposes_on_filter() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()
        composeTestRule.waitForIdle()

        // 1: one filter change → the count value changes once → the label recomposes exactly once.
        composeTestRule.onNodeWithTag("filter_count_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun chip_clear_resets_all() {
        composeTestRule.onNodeWithTag("chip_electronics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("clear_filters_btn").performClick()
        composeTestRule.waitForIdle()

        // 1: clearing flips electronics from selected back to unselected → it recomposes once.
        composeTestRule.onNodeWithTag("chip_electronics").assertRecompositions(exactly = 1)
    }

    @Test
    fun chip_multiple_selections() {
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("chip_electronics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chip_books").performClick()
        composeTestRule.waitForIdle()

        // 1: each chip toggles its own isSelected exactly once across the two clicks.
        composeTestRule.onNodeWithTag("chip_electronics").assertRecompositions(exactly = 1)
        // 1: books is toggled by the second click and recomposes exactly once.
        composeTestRule.onNodeWithTag("chip_books").assertRecompositions(exactly = 1)
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
