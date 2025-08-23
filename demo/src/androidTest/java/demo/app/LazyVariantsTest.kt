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
        // Verify tag resolution in LazyRow subcomposition
        composeTestRule.onNodeWithTag("row_item_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun lazyRow_selectItem_selectionCountRecomposes() {
        composeTestRule.onNodeWithTag("select_row_btn").performClick()
        composeTestRule.onNodeWithTag("row_selection_count").assertRecompositions(exactly = 1)
    }

    @Test
    fun lazyRow_selectItem_gridCountStable() {
        composeTestRule.onNodeWithTag("select_row_btn").performClick()
        composeTestRule.onNodeWithTag("grid_highlight_count").assertStable()
    }

    @Test
    fun lazyRow_initiallyStable() {
        composeTestRule.onNodeWithTag("row_selection_count").assertStable()
    }

    // --- LazyVerticalGrid tests ---

    @Test
    fun lazyGrid_tagMappingWorks() {
        // Verify tag resolution in LazyVerticalGrid subcomposition
        composeTestRule.onNodeWithTag("grid_cell_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun lazyGrid_selectCell_highlightCountRecomposes() {
        composeTestRule.onNodeWithTag("select_grid_btn").performClick()
        composeTestRule.onNodeWithTag("grid_highlight_count").assertRecompositions(exactly = 1)
    }

    @Test
    fun lazyGrid_selectCell_rowCountStable() {
        composeTestRule.onNodeWithTag("select_grid_btn").performClick()
        composeTestRule.onNodeWithTag("row_selection_count").assertStable()
    }

    @Test
    fun lazyGrid_initiallyStable() {
        composeTestRule.onNodeWithTag("grid_highlight_count").assertStable()
    }
}
