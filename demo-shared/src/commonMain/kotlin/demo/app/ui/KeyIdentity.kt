package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import demo.app.GroundTruthCounters
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun KeyIdentityScreen() {
  SideEffect { GroundTruthCounters.increment("key_root") }
  var keyId by remember { mutableIntStateOf(0) }
  var baseValue by remember { mutableIntStateOf(0) }
  var loopCount by remember { mutableIntStateOf(2) }  // starts with 3 items (0..2)

  val derivedA by remember { derivedStateOf { baseValue / 2 } }
  val derivedB by remember { derivedStateOf { derivedA > 0 } }

  Column(modifier = Modifier.testTag("key_root")) {
    key(keyId) {
      KeyedChild(keyId)
    }
    SaveableChild()
    for (i in 0..loopCount) {
      key(i) {
        LoopItem(i)
      }
    }
    LoopCountLabel(loopCount + 1)  // +1 because 0..loopCount has loopCount+1 items
    DerivedChainA(derivedA)
    DerivedChainB(derivedB)
    ChangeKeyButton { keyId++ }
    IncrementBaseButton { baseValue++ }
    IncrementBaseTwiceButton { baseValue += 2 }
    AddLoopItemButton { loopCount++ }
  }
}

@Composable
fun KeyedChild(keyId: Int) {
  SideEffect { GroundTruthCounters.increment("keyed_child") }
  var localCount by remember { mutableIntStateOf(0) }
  Column(modifier = Modifier.testTag("keyed_child")) {
    Text("Key=$keyId local=$localCount")
    Button(
      onClick = { localCount++ },
      modifier = Modifier.testTag("keyed_local_btn")
    ) {
      Text("Local +1")
    }
  }
}

@Composable
fun SaveableChild() {
  SideEffect { GroundTruthCounters.increment("saveable_child") }
  var count by rememberSaveable { mutableIntStateOf(0) }
  Column(modifier = Modifier.testTag("saveable_child")) {
    Text("Saveable: $count")
    Button(
      onClick = { count++ },
      modifier = Modifier.testTag("saveable_inc_btn")
    ) {
      Text("Saveable +1")
    }
  }
}

@Composable
fun LoopItem(index: Int) {
  SideEffect { GroundTruthCounters.increment("loop_item_$index") }
  Text("Loop item $index", modifier = Modifier.testTag("loop_item_$index"))
}

@Composable
fun LoopCountLabel(count: Int) {
  SideEffect { GroundTruthCounters.increment("loop_count_label") }
  Text("Loop items: $count", modifier = Modifier.testTag("loop_count_label"))
}

@Composable
fun DerivedChainA(value: Int) {
  SideEffect { GroundTruthCounters.increment("derived_chain_a") }
  Text("DerivedA: $value", modifier = Modifier.testTag("derived_chain_a"))
}

@Composable
fun DerivedChainB(value: Boolean) {
  SideEffect { GroundTruthCounters.increment("derived_chain_b") }
  Text("DerivedB: $value", modifier = Modifier.testTag("derived_chain_b"))
}

@Composable
fun ChangeKeyButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_key_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_key_btn")) {
    Text("Change Key")
  }
}

@Composable
fun IncrementBaseButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("inc_base_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("inc_base_btn")) {
    Text("Base +1")
  }
}

@Composable
fun IncrementBaseTwiceButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("inc_base_twice_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("inc_base_twice_btn")) {
    Text("Base +2")
  }
}

@Composable
fun AddLoopItemButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("add_loop_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("add_loop_btn")) {
    Text("Add Loop Item")
  }
}
