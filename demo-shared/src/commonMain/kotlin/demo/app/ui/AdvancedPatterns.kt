package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import demo.app.GroundTruthCounters

val LocalNestable = compositionLocalOf { 0 }

@Composable
fun AdvancedPatternsScreen() {
  SideEffect { GroundTruthCounters.increment("advanced_root") }
  var outerValue by remember { mutableIntStateOf(0) }
  var innerValue by remember { mutableIntStateOf(100) }
  var customKey by remember { mutableIntStateOf(0) }
  var visible by remember { mutableStateOf(true) }
  var effectKey by remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.testTag("advanced_root")) {
    // --- CompositionLocal nesting ---
    CompositionLocalProvider(LocalNestable provides outerValue) {
      OuterLocalReader()
      CompositionLocalProvider(LocalNestable provides innerValue) {
        InnerLocalReader()
      }
      OuterLocalReaderB()
    }

    // --- Custom Layout ---
    SimpleCustomLayout {
      CustomLayoutChild()
    }

    // --- Deferred reads (graphicsLayer) ---
    DeferredReadChild(visible)

    // --- Remember with custom key ---
    RememberKeyChild(customKey)

    // --- LaunchedEffect restart ---
    EffectRestartChild(effectKey)

    // --- Buttons ---
    ChangeOuterButton { outerValue++ }
    ChangeInnerButton { innerValue++ }
    ChangeCustomKeyButton { customKey++ }
    ToggleVisibilityAdvButton { visible = !visible }
    ChangeEffectKeyButton { effectKey++ }
  }
}

@Composable
fun OuterLocalReader() {
  SideEffect { GroundTruthCounters.increment("outer_reader") }
  val value = LocalNestable.current
  Text("Outer: $value", modifier = Modifier.testTag("outer_reader"))
}

@Composable
fun InnerLocalReader() {
  SideEffect { GroundTruthCounters.increment("inner_reader") }
  val value = LocalNestable.current
  Text("Inner: $value", modifier = Modifier.testTag("inner_reader"))
}

@Composable
fun OuterLocalReaderB() {
  SideEffect { GroundTruthCounters.increment("outer_reader_b") }
  val value = LocalNestable.current
  Text("OuterB: $value", modifier = Modifier.testTag("outer_reader_b"))
}

@Composable
fun SimpleCustomLayout(content: @Composable () -> Unit) {
  SideEffect { GroundTruthCounters.increment("custom_layout") }
  Layout(modifier = Modifier.testTag("custom_layout"), content = content) { measurables, constraints ->
    val placeables = measurables.map { it.measure(constraints) }
    layout(constraints.maxWidth, placeables.sumOf { it.height }) {
      var y = 0
      placeables.forEach { placeable ->
        placeable.placeRelative(0, y)
        y += placeable.height
      }
    }
  }
}

@Composable
fun CustomLayoutChild() {
  SideEffect { GroundTruthCounters.increment("custom_layout_child") }
  Text("Custom Layout", modifier = Modifier.testTag("custom_layout_child"))
}

@Composable
fun DeferredReadChild(visible: Boolean) {
  SideEffect { GroundTruthCounters.increment("deferred_read") }
  // graphicsLayer reads happen in draw phase, NOT composition phase
  // Changing 'visible' causes a recomposition of this composable because
  // it takes 'visible' as a parameter. But the graphicsLayer block itself
  // does NOT trigger recomposition - it only affects drawing.
  Text(
    "Deferred: $visible",
    modifier = Modifier
      .testTag("deferred_read")
      .graphicsLayer { alpha = if (visible) 1f else 0.5f }
  )
}

@Composable
fun RememberKeyChild(key: Int) {
  SideEffect { GroundTruthCounters.increment("remember_key_child") }
  // When key changes, remember recomputes the value
  val computed = remember(key) { "computed_$key" }
  Text("Remember: $computed", modifier = Modifier.testTag("remember_key_child"))
}

@Composable
fun EffectRestartChild(effectKey: Int) {
  SideEffect { GroundTruthCounters.increment("effect_restart") }
  var effectRan by remember { mutableStateOf(false) }
  LaunchedEffect(effectKey) {
    // Effect restarts when effectKey changes
    effectRan = true
  }
  Text("Effect ran: $effectRan (key=$effectKey)", modifier = Modifier.testTag("effect_restart"))
}

@Composable
fun ChangeOuterButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_outer_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_outer_btn")) {
    Text("Change Outer")
  }
}

@Composable
fun ChangeInnerButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_inner_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_inner_btn")) {
    Text("Change Inner")
  }
}

@Composable
fun ChangeCustomKeyButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_key_btn_adv") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_key_btn_adv")) {
    Text("Change Key")
  }
}

@Composable
fun ToggleVisibilityAdvButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("toggle_vis_adv_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("toggle_vis_adv_btn")) {
    Text("Toggle Vis")
  }
}

@Composable
fun ChangeEffectKeyButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_effect_key_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_effect_key_btn")) {
    Text("Change Effect Key")
  }
}
