@file:OptIn(ExperimentalMaterial3Api::class)

package demo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import demo.app.GroundTruthCounters

@Composable
fun SwipeListScreen() {
  SideEffect { GroundTruthCounters.increment("swipe_list_root") }
  val items = remember { mutableStateListOf("Item A", "Item B", "Item C", "Item D", "Item E") }
  val nextCounter = remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.testTag("swipe_list_root")) {
    ListCountLabel(items.size)
    StaticSwipeLabel()
    Row {
      SwipeAddItemButton {
        nextCounter.intValue++
        items.add("Item X${nextCounter.intValue}")
      }
      Spacer(modifier = Modifier.width(8.dp))
      RemoveFirstButton {
        if (items.isNotEmpty()) items.removeAt(0)
      }
    }
    LazyColumn(modifier = Modifier.weight(1f)) {
      itemsIndexed(items, key = { index, _ -> index }) { index, item ->
        SwipeableItem(
          index = index,
          text = item,
          onDismiss = { items.removeAt(index) }
        )
      }
    }
  }
}

@Composable
fun SwipeableItem(index: Int, text: String, onDismiss: () -> Unit) {
  val tag = "swipeable_item_$index"
  SideEffect { GroundTruthCounters.increment(tag) }
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
      if (value != SwipeToDismissBoxValue.Settled) {
        onDismiss()
        true
      } else {
        false
      }
    }
  )
  SwipeToDismissBox(
    state = dismissState,
    modifier = Modifier
      .testTag(tag)
      .fillMaxWidth(),
    backgroundContent = { DismissBackground(index) },
    content = { ItemContent(index, text) }
  )
}

@Composable
fun ItemContent(index: Int, text: String) {
  val tag = "item_content_$index"
  SideEffect { GroundTruthCounters.increment(tag) }
  Text(
    text = text,
    modifier = Modifier
      .testTag(tag)
      .fillMaxWidth()
      .padding(16.dp)
  )
}

@Composable
fun DismissBackground(index: Int) {
  val tag = "dismiss_background_$index"
  SideEffect { GroundTruthCounters.increment(tag) }
  Box(
    modifier = Modifier
      .testTag(tag)
      .fillMaxSize()
      .background(Color.Red)
      .padding(16.dp),
    contentAlignment = Alignment.CenterEnd
  ) {
    Text(
      text = "\u2716",
      color = Color.White
    )
  }
}

@Composable
fun ListCountLabel(count: Int) {
  SideEffect { GroundTruthCounters.increment("list_count_label") }
  Text(
    text = "Items: $count",
    modifier = Modifier
      .testTag("list_count_label")
      .padding(16.dp)
  )
}

@Composable
fun SwipeAddItemButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("add_item_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("add_item_btn")) {
    Text("Add Item")
  }
}

@Composable
fun RemoveFirstButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("remove_first_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("remove_first_btn")) {
    Text("Remove First")
  }
}

@Composable
fun StaticSwipeLabel() {
  SideEffect { GroundTruthCounters.increment("static_swipe_label") }
  Text(
    text = "Swipe to dismiss",
    modifier = Modifier
      .testTag("static_swipe_label")
      .padding(8.dp)
  )
}
