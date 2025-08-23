package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import demo.app.GroundTruthCounters

@Composable
fun LazyListStressScreen() {
  SideEffect { GroundTruthCounters.increment("lazy_stress_root") }
  val selectedSet = remember { mutableStateOf(setOf<Int>()) }
  val selectedCount by remember { derivedStateOf { selectedSet.value.size } }
  val hasAnySelected by remember { derivedStateOf { selectedSet.value.isNotEmpty() } }

  Column(modifier = Modifier.testTag("lazy_stress_root")) {
    LazyColumn(modifier = Modifier.weight(1f)) {
      item { HeaderItem() }
      item { SelectedCountBanner(selectedCount) }
      item { DerivedBanner(hasAnySelected) }
      items(20, key = { it }) { index ->
        ListItem(
          index = index,
          selected = selectedSet.value.contains(index),
          onToggle = {
            val current = selectedSet.value
            selectedSet.value = if (index in current) current - index else current + index
          }
        )
      }
      item { FooterItem() }
    }
    Row {
      SelectOneButton(onClick = { selectedSet.value = selectedSet.value + 0 })
      SelectAllButton(onClick = { selectedSet.value = (0 until 20).toSet() })
      ClearButton(onClick = { selectedSet.value = emptySet() })
    }
  }
}

@Composable
fun HeaderItem() {
  SideEffect { GroundTruthCounters.increment("list_header") }
  Text("Lazy List Stress", modifier = Modifier.testTag("list_header"))
}

@Composable
fun FooterItem() {
  SideEffect { GroundTruthCounters.increment("list_footer") }
  Text("Footer", modifier = Modifier.testTag("list_footer"))
}

@Composable
fun SelectedCountBanner(count: Int) {
  SideEffect { GroundTruthCounters.increment("selected_banner") }
  Text("Selected: $count", modifier = Modifier.testTag("selected_banner"))
}

@Composable
fun DerivedBanner(hasAny: Boolean) {
  SideEffect { GroundTruthCounters.increment("derived_banner") }
  Text(
    if (hasAny) "Has selection" else "No selection",
    modifier = Modifier.testTag("derived_banner")
  )
}

@Composable
fun ListItem(index: Int, selected: Boolean, onToggle: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("item_$index") }
  Button(
    onClick = onToggle,
    modifier = Modifier.testTag("item_$index")
  ) {
    Text(if (selected) "Item $index [selected]" else "Item $index")
  }
}

@Composable
fun SelectOneButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("select_0_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("select_0_btn")) {
    Text("Select #0")
  }
}

@Composable
fun SelectAllButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("select_all_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("select_all_btn")) {
    Text("Select All")
  }
}

@Composable
fun ClearButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("clear_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("clear_btn")) {
    Text("Clear")
  }
}
