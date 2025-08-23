package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import demo.app.GroundTruthCounters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InputScrollScreen() {
  SideEffect { GroundTruthCounters.increment("input_scroll_root") }
  var text by remember { mutableStateOf("") }
  var sourceValue by remember { mutableIntStateOf(0) }
  val lazyListState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  val snapshotFlowValue by remember {
    snapshotFlow { sourceValue }
  }.collectAsState(initial = 0)

  Column(modifier = Modifier.testTag("input_scroll_root")) {
    TextField(
      value = text,
      onValueChange = { text = it },
      modifier = Modifier.testTag("text_field")
    )
    TextDisplay(text)
    UnrelatedToText()

    LazyColumn(
      state = lazyListState,
      modifier = Modifier
        .height(200.dp)
        .testTag("scroll_list")
    ) {
      items(30) { index ->
        ScrollItem(index)
      }
    }

    ScrollPositionReader(lazyListState.firstVisibleItemIndex)
    ProducedValueDisplay(sourceValue)
    SnapshotFlowReader(snapshotFlowValue)

    TypeCharButton { text += "a" }
    ScrollDownButton { coroutineScope.launch { lazyListState.animateScrollToItem(15) } }
    ChangeSourceButton { sourceValue++ }
    SameSourceButton { sourceValue = sourceValue }
  }
}

@Composable
fun TextDisplay(text: String) {
  SideEffect { GroundTruthCounters.increment("text_display") }
  Text("Text: $text", modifier = Modifier.testTag("text_display"))
}

@Composable
fun UnrelatedToText() {
  SideEffect { GroundTruthCounters.increment("text_unrelated") }
  Text("Unrelated", modifier = Modifier.testTag("text_unrelated"))
}

@Composable
fun ScrollItem(index: Int) {
  SideEffect { GroundTruthCounters.increment("scroll_item_$index") }
  Text("Scroll item $index", modifier = Modifier.testTag("scroll_item_$index"))
}

@Composable
fun ScrollPositionReader(position: Int) {
  SideEffect { GroundTruthCounters.increment("scroll_position") }
  Text("Position: $position", modifier = Modifier.testTag("scroll_position"))
}

@Composable
fun ProducedValueDisplay(trigger: Int) {
  SideEffect { GroundTruthCounters.increment("produced_value") }
  val produced by produceState(initialValue = 0, trigger) {
    delay(100)
    value = trigger + 1
  }
  Text("Produced: $produced", modifier = Modifier.testTag("produced_value"))
}

@Composable
fun SnapshotFlowReader(value: Int) {
  SideEffect { GroundTruthCounters.increment("flow_reader") }
  Text("Flow: $value", modifier = Modifier.testTag("flow_reader"))
}

@Composable
fun TypeCharButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("type_char_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("type_char_btn")) {
    Text("Type")
  }
}

@Composable
fun ScrollDownButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("scroll_down_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("scroll_down_btn")) {
    Text("Scroll Down")
  }
}

@Composable
fun ChangeSourceButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_source_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_source_btn")) {
    Text("Change Source")
  }
}

@Composable
fun SameSourceButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("same_source_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("same_source_btn")) {
    Text("Same Source")
  }
}
