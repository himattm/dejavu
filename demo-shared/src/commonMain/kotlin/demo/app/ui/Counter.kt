package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import demo.app.GroundTruthCounters
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun CounterScreen() {
  SideEffect { GroundTruthCounters.increment("counter_screen") }
  var count by remember { mutableIntStateOf(0) }
  Column(modifier = Modifier.testTag("counter_screen")) {
    CounterTitle()
    CounterValue(count)
    IncButton { count++ }
    ResetButton { count = 0 }
  }
}

@Composable fun CounterTitle() {
  SideEffect { GroundTruthCounters.increment("counter_title") }
  Text("Dejavu Counter", modifier = Modifier.testTag("counter_title"))
}

@Composable fun CounterValue(value: Int) {
  SideEffect { GroundTruthCounters.increment("counter_value") }
  Text("Value: $value", modifier = Modifier.testTag("counter_value"))
}

@Composable fun IncButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("inc_button") }
  Button(onClick = onClick, modifier = Modifier.testTag("inc_button")) {
    Text("Inc") 
  }
}

@Composable fun ResetButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("reset_button") }
  Button(onClick = onClick, modifier = Modifier.testTag("reset_button")) {
    Text("Reset") 
  }
}
