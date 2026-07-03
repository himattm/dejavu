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
class LazyVariantsTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<LazyVariantsActivity>()

    // --- LazyRow tests ---

    @Test
    fun lazyRow_tagMappingWorks() {
        // Verify tag resolution in LazyRow subcomposition.
        // Android resolves per-instance; with no interaction row_item_0 never recomposes.
        composeTestRule.onNodeWithTag("row_item_0").assertStable()
    }

    @Test
    fun lazyRow_selectItem_selectionCountRecomposes() {
        composeTestRule.onNodeWithTag("select_row_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: select_row_btn changes selectedRowItems.size 0 -> 1; RowSelectionCount reads .size, recomposes once.
        composeTestRule.onNodeWithTag("row_selection_count").assertRecompositions(exactly = 1)
    }

    @Test
    fun lazyRow_selectItem_gridCountStable() {
        composeTestRule.onNodeWithTag("select_row_btn").performClick()
        composeTestRule.waitForIdle()
        // A row-only change never touches highlightedGridCells, so GridHighlightCount stays stable.
        composeTestRule.onNodeWithTag("grid_highlight_count").assertStable()
    }

    @Test
    fun lazyRow_initiallyStable() {
        // No interaction: the single-instance row count never recomposes.
        composeTestRule.onNodeWithTag("row_selection_count").assertStable()
    }

    // --- LazyVerticalGrid tests ---

    @Test
    fun lazyGrid_tagMappingWorks() {
        // Verify tag resolution in LazyVerticalGrid subcomposition.
        // Android resolves per-instance; with no interaction grid_cell_0 never recomposes.
        composeTestRule.onNodeWithTag("grid_cell_0").assertStable()
    }

    @Test
    fun lazyGrid_selectCell_highlightCountRecomposes() {
        composeTestRule.onNodeWithTag("select_grid_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: select_grid_btn changes highlightedGridCells.size 0 -> 1; GridHighlightCount reads .size, recomposes once.
        composeTestRule.onNodeWithTag("grid_highlight_count").assertRecompositions(exactly = 1)
    }

    @Test
    fun lazyGrid_selectCell_rowCountStable() {
        composeTestRule.onNodeWithTag("select_grid_btn").performClick()
        composeTestRule.waitForIdle()
        // A grid-only change never touches selectedRowItems, so RowSelectionCount stays stable.
        composeTestRule.onNodeWithTag("row_selection_count").assertStable()
    }

    @Test
    fun lazyGrid_initiallyStable() {
        // No interaction: the single-instance grid count never recomposes.
        composeTestRule.onNodeWithTag("grid_highlight_count").assertStable()
    }
}
