package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import demo.app.GroundTruthCounters

val LocalStaticConfig = staticCompositionLocalOf { 0 }

@Composable
fun DialogPopupScreen() {
  SideEffect { GroundTruthCounters.increment("dialog_root") }
  var staticConfig by remember { mutableIntStateOf(0) }
  var dynamicValue by remember { mutableIntStateOf(0) }
  var showDialog by remember { mutableStateOf(false) }
  var showPopup by remember { mutableStateOf(false) }

  CompositionLocalProvider(LocalStaticConfig provides staticConfig) {
    Column(modifier = Modifier.testTag("dialog_root")) {
      StaticReaderA()
      StaticReaderB()
      StaticReaderC()
      DynamicReaderD(dynamicValue)
      UnrelatedStaticChild()
      if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
          DialogContent()
        }
      }
      if (showPopup) {
        Popup(onDismissRequest = { showPopup = false }) {
          PopupContent()
        }
      }
      ShowDialogButton { showDialog = true }
      ShowPopupButton { showPopup = true }
      ChangeStaticButton { staticConfig++ }
      ChangeDynamicButton { dynamicValue++ }
    }
  }
}

@Composable
fun StaticReaderA() {
  SideEffect { GroundTruthCounters.increment("static_reader_a") }
  val config = LocalStaticConfig.current
  Text("StaticA: $config", modifier = Modifier.testTag("static_reader_a"))
}

@Composable
fun StaticReaderB() {
  SideEffect { GroundTruthCounters.increment("static_reader_b") }
  val config = LocalStaticConfig.current
  Text("StaticB: $config", modifier = Modifier.testTag("static_reader_b"))
}

@Composable
fun StaticReaderC() {
  SideEffect { GroundTruthCounters.increment("static_reader_c") }
  val config = LocalStaticConfig.current
  Text("StaticC: $config", modifier = Modifier.testTag("static_reader_c"))
}

@Composable
fun DynamicReaderD(value: Int) {
  SideEffect { GroundTruthCounters.increment("dynamic_reader_d") }
  Text("Dynamic: $value", modifier = Modifier.testTag("dynamic_reader_d"))
}

@Composable
fun UnrelatedStaticChild() {
  SideEffect { GroundTruthCounters.increment("unrelated_static") }
  Text("Unrelated", modifier = Modifier.testTag("unrelated_static"))
}

@Composable
fun DialogContent() {
  SideEffect { GroundTruthCounters.increment("dialog_content") }
  Column(modifier = Modifier.testTag("dialog_content")) {
    Text("Dialog Content")
    DialogInner()
  }
}

@Composable
fun DialogInner() {
  SideEffect { GroundTruthCounters.increment("dialog_inner") }
  Text("Dialog Inner", modifier = Modifier.testTag("dialog_inner"))
}

@Composable
fun PopupContent() {
  SideEffect { GroundTruthCounters.increment("popup_content") }
  Text("Popup Content", modifier = Modifier.testTag("popup_content"))
}

@Composable
fun ShowDialogButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("show_dialog_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("show_dialog_btn")) {
    Text("Show Dialog")
  }
}

@Composable
fun ShowPopupButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("show_popup_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("show_popup_btn")) {
    Text("Show Popup")
  }
}

@Composable
fun ChangeStaticButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_static_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_static_btn")) {
    Text("Change Static")
  }
}

@Composable
fun ChangeDynamicButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_dynamic_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_dynamic_btn")) {
    Text("Change Dynamic")
  }
}
