package demo.app.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import demo.app.GroundTruthCounters
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LazyVariantsScreen() {
  SideEffect { GroundTruthCounters.increment("lazy_variants_root") }
  var selectedRowItems by remember { mutableStateOf(setOf<Int>()) }
  var highlightedGridCells by remember { mutableStateOf(setOf<Int>()) }

  Column(modifier = Modifier.testTag("lazy_variants_root")) {
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
fun RowItem(index: Int, selected: Boolean) {
  SideEffect { GroundTruthCounters.increment("row_item_$index") }
  Text(
    text = if (selected) "R$index*" else "R$index",
    modifier = Modifier
      .testTag("row_item_$index")
      .width(50.dp)
      .padding(4.dp)
  )
}

@Composable
fun GridCell(index: Int, highlighted: Boolean) {
  SideEffect { GroundTruthCounters.increment("grid_cell_$index") }
  Text(
    text = if (highlighted) "G$index*" else "G$index",
    modifier = Modifier
      .testTag("grid_cell_$index")
      .padding(8.dp)
  )
}

@Composable
fun RowSelectionCount(count: Int) {
  SideEffect { GroundTruthCounters.increment("row_selection_count") }
  Text("Row selected: $count", modifier = Modifier.testTag("row_selection_count"))
}

@Composable
fun GridHighlightCount(count: Int) {
  SideEffect { GroundTruthCounters.increment("grid_highlight_count") }
  Text("Grid highlighted: $count", modifier = Modifier.testTag("grid_highlight_count"))
}

@Composable
fun SelectRowItemButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("select_row_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("select_row_btn")) {
    Text("Select Row 0")
  }
}

@Composable
fun ClearRowButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("clear_row_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("clear_row_btn")) {
    Text("Clear Row")
  }
}

@Composable
fun SelectGridCellButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("select_grid_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("select_grid_btn")) {
    Text("Select Grid 0")
  }
}

@Composable
fun ClearGridButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("clear_grid_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("clear_grid_btn")) {
    Text("Clear Grid")
  }
}
