package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import demo.app.GroundTruthCounters
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlowStateHolder {
  private val _count = MutableStateFlow(0)
  val count: StateFlow<Int> = _count.asStateFlow()

  fun increment() {
    _count.value++
  }

  fun incrementThreeTimes() {
    _count.value = _count.value + 1
    _count.value = _count.value + 1
    _count.value = _count.value + 1
  }
}

@Composable
fun FlowStateScreen() {
  SideEffect { GroundTruthCounters.increment("flow_root") }
  val holder = remember { FlowStateHolder() }
  var stateA by remember { mutableIntStateOf(0) }
  var stateB by remember { mutableIntStateOf(0) }
  val items = remember { mutableStateListOf<String>() }

  Column(modifier = Modifier.testTag("flow_root")) {
    FlowCounter(holder)
    SameValueReader(stateA)
    CoalescingReader(stateA)
    ListSizeReader(items.size)
    BatchReaderA(stateA)
    BatchReaderB(stateB)
    FlowIncButton { holder.increment() }
    FlowIncThreeButton { holder.incrementThreeTimes() }
    SameValueWriteButton { stateA = stateA }
    RealValueWriteButton { stateA++ }
    AddItemButton { items.add("Item ${items.size}") }
    CoalesceButton { stateA++; stateB++ }
    BatchButton { Snapshot.withMutableSnapshot { stateA = 100; stateB = 200 } }
  }
}

@Composable
fun FlowCounter(holder: FlowStateHolder) {
  SideEffect { GroundTruthCounters.increment("flow_counter") }
  val count by holder.count.collectAsState()
  Text("Flow: $count", modifier = Modifier.testTag("flow_counter"))
}

@Composable
fun SameValueReader(value: Int) {
  SideEffect { GroundTruthCounters.increment("same_value_reader") }
  Text("Same: $value", modifier = Modifier.testTag("same_value_reader"))
}

@Composable
fun CoalescingReader(value: Int) {
  SideEffect { GroundTruthCounters.increment("coalescing_reader") }
  Text("Coalesce: $value", modifier = Modifier.testTag("coalescing_reader"))
}

@Composable
fun ListSizeReader(size: Int) {
  SideEffect { GroundTruthCounters.increment("list_size_reader") }
  Text("Items: $size", modifier = Modifier.testTag("list_size_reader"))
}

@Composable
fun BatchReaderA(value: Int) {
  SideEffect { GroundTruthCounters.increment("batch_reader_a") }
  Text("BatchA: $value", modifier = Modifier.testTag("batch_reader_a"))
}

@Composable
fun BatchReaderB(value: Int) {
  SideEffect { GroundTruthCounters.increment("batch_reader_b") }
  Text("BatchB: $value", modifier = Modifier.testTag("batch_reader_b"))
}

@Composable
fun FlowIncButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("flow_inc_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("flow_inc_btn")) {
    Text("Flow +1")
  }
}

@Composable
fun FlowIncThreeButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("flow_inc_three_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("flow_inc_three_btn")) {
    Text("Flow +3")
  }
}

@Composable
fun SameValueWriteButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("same_value_write_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("same_value_write_btn")) {
    Text("Same Write")
  }
}

@Composable
fun RealValueWriteButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("real_value_write_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("real_value_write_btn")) {
    Text("Real Write")
  }
}

@Composable
fun AddItemButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("add_item_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("add_item_btn")) {
    Text("Add Item")
  }
}

@Composable
fun CoalesceButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("coalesce_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("coalesce_btn")) {
    Text("Coalesce")
  }
}

@Composable
fun BatchButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("batch_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("batch_btn")) {
    Text("Batch")
  }
}
