package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Cross-platform port of Android LazyVariantsTest.
 * LazyRow + LazyVerticalGrid with set-based selection.
 */
@OptIn(ExperimentalTestApi::class)
class LazyVariantsPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    // --- LazyRow tests ---

    @Test
    fun lazyRow_tagMappingWorks() = runComposeUiTest {
        setContent { DejavuTestContent { LazyVariantsScreen() } }
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("row_item_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun lazyRow_selectItem_selectionCountRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { LazyVariantsScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("select_row_btn").performClick()
        waitForIdle()

        onNodeWithTag("row_selection_count").assertRecompositions(exactly = 1)
    }

    @Test
    fun lazyRow_selectItem_gridCountStable() = runComposeUiTest {
        setContent { DejavuTestContent { LazyVariantsScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("select_row_btn").performClick()
        waitForIdle()

        onNodeWithTag("grid_highlight_count").assertStable()
    }

    @Test
    fun lazyRow_initiallyStable() = runComposeUiTest {
        setContent { DejavuTestContent { LazyVariantsScreen() } }
        waitForIdle()

        onNodeWithTag("row_selection_count").assertStable()
    }

    // --- LazyVerticalGrid tests ---

    @Test
    fun lazyGrid_tagMappingWorks() = runComposeUiTest {
        setContent { DejavuTestContent { LazyVariantsScreen() } }
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("grid_cell_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun lazyGrid_selectCell_highlightCountRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { LazyVariantsScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("select_grid_btn").performClick()
        waitForIdle()

        onNodeWithTag("grid_highlight_count").assertRecompositions(exactly = 1)
    }

    @Test
    fun lazyGrid_selectCell_rowCountStable() = runComposeUiTest {
        setContent { DejavuTestContent { LazyVariantsScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("select_grid_btn").performClick()
        waitForIdle()

        onNodeWithTag("row_selection_count").assertStable()
    }

    @Test
    fun lazyGrid_initiallyStable() = runComposeUiTest {
        setContent { DejavuTestContent { LazyVariantsScreen() } }
        waitForIdle()

        onNodeWithTag("grid_highlight_count").assertStable()
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun LazyVariantsScreen() {
    var selectedRowItems by remember { mutableStateOf(setOf<Int>()) }
    var highlightedGridCells by remember { mutableStateOf(setOf<Int>()) }

    Column {
        LazyRow(
            modifier = Modifier.height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(10, key = { it }) { index ->
                RowItem(index, selectedRowItems.contains(index))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(200.dp)
        ) {
            items(8, key = { it }) { index ->
                GridCell(index, highlightedGridCells.contains(index))
            }
        }

        RowSelectionCount(selectedRowItems.size)
        GridHighlightCount(highlightedGridCells.size)

        Row {
            SelectRowItemButton { selectedRowItems = selectedRowItems + 0 }
            ClearRowButton { selectedRowItems = emptySet() }
        }
        Row {
            SelectGridCellButton { highlightedGridCells = highlightedGridCells + 0 }
            ClearGridButton { highlightedGridCells = emptySet() }
        }
    }
}

@Composable
private fun RowItem(index: Int, selected: Boolean) {
    BasicText(
        text = if (selected) "R$index*" else "R$index",
        modifier = Modifier
            .testTag("row_item_$index")
            .width(50.dp)
            .padding(4.dp)
    )
}

@Composable
private fun GridCell(index: Int, highlighted: Boolean) {
    BasicText(
        text = if (highlighted) "G$index*" else "G$index",
        modifier = Modifier
            .testTag("grid_cell_$index")
            .padding(8.dp)
    )
}

@Composable
private fun RowSelectionCount(count: Int) {
    BasicText("Row selected: $count", modifier = Modifier.testTag("row_selection_count"))
}

@Composable
private fun GridHighlightCount(count: Int) {
    BasicText("Grid highlighted: $count", modifier = Modifier.testTag("grid_highlight_count"))
}

@Composable
private fun SelectRowItemButton(onClick: () -> Unit) {
    BasicText("Select Row 0", Modifier.testTag("select_row_btn").clickable(onClick = onClick))
}

@Composable
private fun ClearRowButton(onClick: () -> Unit) {
    BasicText("Clear Row", Modifier.testTag("clear_row_btn").clickable(onClick = onClick))
}

@Composable
private fun SelectGridCellButton(onClick: () -> Unit) {
    BasicText("Select Grid 0", Modifier.testTag("select_grid_btn").clickable(onClick = onClick))
}

@Composable
private fun ClearGridButton(onClick: () -> Unit) {
    BasicText("Clear Grid", Modifier.testTag("clear_grid_btn").clickable(onClick = onClick))
}
