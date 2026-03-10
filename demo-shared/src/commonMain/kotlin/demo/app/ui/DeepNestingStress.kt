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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import demo.app.GroundTruthCounters

@Composable
fun DeepNestingStressScreen() {
  SideEffect { GroundTruthCounters.increment("deep_root") }
  var count by remember { mutableIntStateOf(0) }
  Column(modifier = Modifier.testTag("deep_root")) {
    Level1Container(count)
    SiblingBranch()
    Button(onClick = { count++ }, modifier = Modifier.testTag("deep_inc_btn")) {
      Text("Increment")
    }
  }
}

@Composable fun Level1Container(count: Int) {
  SideEffect { GroundTruthCounters.increment("level_1") }
  Column(modifier = Modifier.testTag("level_1")) {
    Text("Level 1: $count")
    Level2Panel(count)
  }
}

@Composable fun Level2Panel(count: Int) {
  SideEffect { GroundTruthCounters.increment("level_2") }
  Column(modifier = Modifier.testTag("level_2")) {
    Text("Level 2: $count")
    Level3Section(count)
  }
}

@Composable fun Level3Section(count: Int) {
  SideEffect { GroundTruthCounters.increment("level_3") }
  Column(modifier = Modifier.testTag("level_3")) {
    Text("Level 3: $count")
    Level4Card(count)
  }
}

@Composable fun Level4Card(count: Int) {
  SideEffect { GroundTruthCounters.increment("level_4") }
  Column(modifier = Modifier.testTag("level_4")) {
    Text("Level 4: $count")
    Level5Detail(count)
  }
}

@Composable fun Level5Detail(count: Int) {
  SideEffect { GroundTruthCounters.increment("level_5") }
  Column(modifier = Modifier.testTag("level_5")) {
    Text("Level 5: $count")
    Level6Leaf(count)
  }
}

@Composable fun Level6Leaf(count: Int) {
  SideEffect { GroundTruthCounters.increment("level_6") }
  Text("Level 6: $count", modifier = Modifier.testTag("level_6"))
}

@Composable fun SiblingBranch() {
  SideEffect { GroundTruthCounters.increment("sibling_branch") }
  Column(modifier = Modifier.testTag("sibling_branch")) {
    Text("Sibling Branch")
    SiblingChild()
  }
}

@Composable fun SiblingChild() {
  SideEffect { GroundTruthCounters.increment("sibling_child") }
  Text("Sibling Child", modifier = Modifier.testTag("sibling_child"))
}
