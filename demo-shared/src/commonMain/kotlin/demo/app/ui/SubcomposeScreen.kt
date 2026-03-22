package demo.app.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import demo.app.GroundTruthCounters

@Composable
fun SubcomposeScreenRoot() {
    SideEffect { GroundTruthCounters.increment("subcompose_root") }
    var widthToggle by remember { mutableStateOf(false) }
    var animTarget by remember { mutableIntStateOf(0) }
    var movableSlot by remember { mutableStateOf("A") }
    var triggerParent by remember { mutableIntStateOf(0) }

    // Force read triggerParent so incrementing it recomposes this scope
    val parentTrigger = triggerParent

    val animatedValue by animateIntAsState(
        targetValue = animTarget,
        animationSpec = tween(durationMillis = 300),
        label = "anim"
    )

    val movableChild = remember {
        movableContentOf {
            MovableChild()
        }
    }

    Column(modifier = Modifier.testTag("subcompose_root")) {
        Box(modifier = Modifier.fillMaxWidth(if (widthToggle) 1f else 0.5f)) {
            BoxWithConstraints {
                ConstraintReader(maxWidth.value.toInt())
            }
        }

        AnimValueReader(animatedValue)

        // Movable content in two slots
        if (movableSlot == "A") {
            Box(modifier = Modifier.testTag("slot_a")) {
                movableChild()
            }
        } else {
            Box(modifier = Modifier.testTag("slot_b")) {
                movableChild()
            }
        }

        NonRestartableChild(parentTrigger)
        RegularChild(parentTrigger)

        ToggleWidthButton { widthToggle = !widthToggle }
        AnimateButton { animTarget += 100 }
        MoveContentButton { movableSlot = if (movableSlot == "A") "B" else "A" }
        TriggerParentButton { triggerParent++ }
    }
}

@Composable
fun ConstraintReader(maxWidthPx: Int) {
    SideEffect { GroundTruthCounters.increment("constraint_reader") }
    Text("MaxWidth: $maxWidthPx", modifier = Modifier.testTag("constraint_reader"))
}

@Composable
fun AnimValueReader(value: Int) {
    SideEffect { GroundTruthCounters.increment("anim_value_reader") }
    Text("Anim: $value", modifier = Modifier.testTag("anim_value_reader"))
}

@Composable
fun MovableChild() {
    SideEffect { GroundTruthCounters.increment("movable_child") }
    var localState by remember { mutableIntStateOf(0) }
    Text("Movable: $localState", modifier = Modifier.testTag("movable_child"))
}

@NonRestartableComposable
@Composable
fun NonRestartableChild(value: Int) {
    SideEffect { GroundTruthCounters.increment("non_restartable") }
    Text("NonRestartable: $value", modifier = Modifier.testTag("non_restartable"))
}

@Composable
fun RegularChild(value: Int) {
    SideEffect { GroundTruthCounters.increment("regular_child") }
    Text("Regular: $value", modifier = Modifier.testTag("regular_child"))
}

@Composable
fun ToggleWidthButton(onClick: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("toggle_width_btn") }
    Button(onClick = onClick, modifier = Modifier.testTag("toggle_width_btn")) {
        Text("Toggle Width")
    }
}

@Composable
fun AnimateButton(onClick: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("animate_btn") }
    Button(onClick = onClick, modifier = Modifier.testTag("animate_btn")) {
        Text("Animate")
    }
}

@Composable
fun MoveContentButton(onClick: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("move_content_btn") }
    Button(onClick = onClick, modifier = Modifier.testTag("move_content_btn")) {
        Text("Move Content")
    }
}

@Composable
fun TriggerParentButton(onClick: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("trigger_parent_btn") }
    Button(onClick = onClick, modifier = Modifier.testTag("trigger_parent_btn")) {
        Text("Trigger Parent")
    }
}
