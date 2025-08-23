package demo.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import demo.app.GroundTruthCounters

@Composable
fun ToggleMorphScreen() {
  SideEffect { GroundTruthCounters.increment("toggle_morph_root") }
  var isOn by remember { mutableStateOf(false) }

  Column(modifier = Modifier.testTag("toggle_morph_root")) {
    MorphToggle(isOn)
    Spacer(modifier = Modifier.height(16.dp))
    ToggleLabel(isOn)
    Spacer(modifier = Modifier.height(8.dp))
    ToggleButton { isOn = !isOn }
    Spacer(modifier = Modifier.height(8.dp))
    StaticMorphSibling()
  }
}

@Composable
fun MorphToggle(isOn: Boolean) {
  SideEffect { GroundTruthCounters.increment("morph_toggle") }
  Box(modifier = Modifier.testTag("morph_toggle")) {
    ToggleTrack(isOn)
    ToggleThumb(isOn)
  }
}

@Composable
fun ToggleThumb(isOn: Boolean) {
  SideEffect { GroundTruthCounters.increment("toggle_thumb") }
  val offsetX by animateDpAsState(
    targetValue = if (isOn) 24.dp else 0.dp,
    label = "thumbOffset"
  )
  val thumbColor by animateColorAsState(
    targetValue = if (isOn) Color.Green else Color.Gray,
    label = "thumbColor"
  )
  val cornerPercent by animateIntAsState(
    targetValue = if (isOn) 20 else 50,
    label = "thumbCorner"
  )
  Box(
    modifier = Modifier
      .testTag("toggle_thumb")
      .offset(x = offsetX)
      .size(24.dp)
      .clip(RoundedCornerShape(cornerPercent))
      .background(thumbColor)
  )
}

@Composable
fun ToggleTrack(isOn: Boolean) {
  SideEffect { GroundTruthCounters.increment("toggle_track") }
  val trackColor by animateColorAsState(
    targetValue = if (isOn) Color.Green.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f),
    label = "trackColor"
  )
  Box(
    modifier = Modifier
      .testTag("toggle_track")
      .size(width = 48.dp, height = 24.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(trackColor)
  )
}

@Composable
fun ToggleLabel(isOn: Boolean) {
  SideEffect { GroundTruthCounters.increment("toggle_label") }
  Text(
    text = if (isOn) "ON" else "OFF",
    modifier = Modifier.testTag("toggle_label")
  )
}

@Composable
fun ToggleButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("toggle_btn") }
  Button(
    onClick = onClick,
    modifier = Modifier.testTag("toggle_btn")
  ) {
    Text("Toggle")
  }
}

@Composable
fun StaticMorphSibling() {
  SideEffect { GroundTruthCounters.increment("static_morph_sibling") }
  Text(
    text = "I am static",
    modifier = Modifier.testTag("static_morph_sibling")
  )
}
