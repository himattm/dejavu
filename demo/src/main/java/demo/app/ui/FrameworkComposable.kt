package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import demo.app.GroundTruthCounters

@Composable
fun FrameworkComposableScreen() {
    SideEffect { GroundTruthCounters.increment("fw_screen_root") }
    var outlinedText by remember { mutableStateOf("") }
    var textFieldText by remember { mutableStateOf("") }
    var switchState by remember { mutableStateOf(false) }
    var counter by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.testTag("fw_screen_root")) {
        TaskDescriptionInput(
            value = outlinedText,
            onValueChanged = { outlinedText = it },
        )
        SimpleTextInput(
            value = textFieldText,
            onValueChanged = { textFieldText = it },
        )
        ToggleSwitch(checked = switchState, onCheckedChange = { switchState = it })
        InfoCard(counter)
        CounterLabel(counter)

        TypeCharTrigger {
            outlinedText += "a"
            textFieldText += "b"
        }
        ToggleTrigger { switchState = !switchState }
        IncrementTrigger { counter++ }
    }
}

@Composable
fun TaskDescriptionInput(value: String, onValueChanged: (String) -> Unit) {
    SideEffect { GroundTruthCounters.increment("task_description_input") }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        label = { Text("Description") },
        modifier = Modifier.testTag("task_description_input"),
    )
}

@Composable
fun SimpleTextInput(value: String, onValueChanged: (String) -> Unit) {
    SideEffect { GroundTruthCounters.increment("simple_text_input") }
    TextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = Modifier.testTag("simple_text_input"),
    )
}

@Composable
fun ToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SideEffect { GroundTruthCounters.increment("toggle_switch") }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag("toggle_switch"),
    )
}

@Composable
fun InfoCard(counter: Int) {
    SideEffect { GroundTruthCounters.increment("info_card") }
    Card(modifier = Modifier.testTag("info_card")) {
        Text("Card content: $counter")
    }
}

@Composable
fun CounterLabel(counter: Int) {
    SideEffect { GroundTruthCounters.increment("counter_label") }
    Text("Counter: $counter", modifier = Modifier.testTag("counter_label"))
}

@Composable
private fun TypeCharTrigger(onClick: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("type_char_trigger") }
    Button(onClick = onClick, modifier = Modifier.testTag("type_char_trigger")) {
        Text("Type Char")
    }
}

@Composable
private fun ToggleTrigger(onClick: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("toggle_trigger") }
    Button(onClick = onClick, modifier = Modifier.testTag("toggle_trigger")) {
        Text("Toggle")
    }
}

@Composable
private fun IncrementTrigger(onClick: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("increment_trigger") }
    Button(onClick = onClick, modifier = Modifier.testTag("increment_trigger")) {
        Text("Increment")
    }
}
