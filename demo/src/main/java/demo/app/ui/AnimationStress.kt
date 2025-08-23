package demo.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import demo.app.GroundTruthCounters
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun AnimationStressScreen() {
  SideEffect { GroundTruthCounters.increment("anim_stress_root") }
  var visible by remember { mutableStateOf(false) }
  var contentState by remember { mutableIntStateOf(0) }
  var showConditional by remember { mutableStateOf(false) }
  var bannerPulse by remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.testTag("anim_stress_root")) {
    ToggleVisibilityButton { visible = !visible }
    AnimatedVisibility(visible = visible) {
      VisiblePanel()
    }
    CycleContentButton { contentState = (contentState + 1) % 3 }
    AnimatedContent(targetState = contentState, label = "content") { state ->
      when (state) {
        0 -> ContentVariantA()
        1 -> ContentVariantB()
        else -> ContentVariantC()
      }
    }
    ToggleConditionalButton { showConditional = !showConditional }
    if (showConditional) {
      ConditionalChild()
    }
    PulseBannerButton { bannerPulse++ }
    AnimatingBanner(bannerPulse)
    StaticLabel()
  }
}

@Composable
fun ToggleVisibilityButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("toggle_vis_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("toggle_vis_btn")) {
    Text("Toggle Visibility")
  }
}

@Composable
fun VisiblePanel() {
  SideEffect { GroundTruthCounters.increment("visible_panel") }
  Text("Visible Panel", modifier = Modifier.testTag("visible_panel"))
}

@Composable
fun CycleContentButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("cycle_content_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("cycle_content_btn")) {
    Text("Cycle Content")
  }
}

@Composable
fun ContentVariantA() {
  SideEffect { GroundTruthCounters.increment("variant_a") }
  Text("Variant A", modifier = Modifier.testTag("variant_a"))
}

@Composable
fun ContentVariantB() {
  SideEffect { GroundTruthCounters.increment("variant_b") }
  Text("Variant B", modifier = Modifier.testTag("variant_b"))
}

@Composable
fun ContentVariantC() {
  SideEffect { GroundTruthCounters.increment("variant_c") }
  Text("Variant C", modifier = Modifier.testTag("variant_c"))
}

@Composable
fun ToggleConditionalButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("toggle_cond_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("toggle_cond_btn")) {
    Text("Toggle Conditional")
  }
}

@Composable
fun ConditionalChild() {
  SideEffect { GroundTruthCounters.increment("conditional_child") }
  Text("Conditional", modifier = Modifier.testTag("conditional_child"))
}

@Composable
fun PulseBannerButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("pulse_banner_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("pulse_banner_btn")) {
    Text("Pulse Banner")
  }
}

@Composable
fun AnimatingBanner(pulse: Int) {
  SideEffect { GroundTruthCounters.increment("animating_banner") }
  Text(
    "Animating pulse=$pulse",
    modifier = Modifier.testTag("animating_banner")
  )
}

@Composable
fun StaticLabel() {
  SideEffect { GroundTruthCounters.increment("static_label") }
  Text("Static Label", modifier = Modifier.testTag("static_label"))
}
