package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import demo.app.GroundTruthCounters

val LocalThemeMode = compositionLocalOf { false }

@Composable
fun SharedStateStressScreen() {
  SideEffect { GroundTruthCounters.increment("shared_root") }
  var counterA by remember { mutableIntStateOf(0) }
  var counterB by remember { mutableIntStateOf(0) }
  var counterC by remember { mutableIntStateOf(0) }
  var themeMode by remember { mutableStateOf(false) }

  CompositionLocalProvider(LocalThemeMode provides themeMode) {
    Column(modifier = Modifier.testTag("shared_root")) {
      CounterAReader(counterA)
      CounterBReader(counterB)
      CounterCReader(counterC)
      DualReader(counterA, counterB)
      ThemeReader()
      AllReader(counterA, counterB, counterC)
      IncAButton { counterA++ }
      IncBButton { counterB++ }
      IncCButton { counterC++ }
      ToggleThemeButton { themeMode = !themeMode }
    }
  }
}

@Composable fun CounterAReader(value: Int) {
  SideEffect { GroundTruthCounters.increment("reader_a") }
  Text("A: $value", modifier = Modifier.testTag("reader_a"))
}

@Composable fun CounterBReader(value: Int) {
  SideEffect { GroundTruthCounters.increment("reader_b") }
  Text("B: $value", modifier = Modifier.testTag("reader_b"))
}

@Composable fun CounterCReader(value: Int) {
  SideEffect { GroundTruthCounters.increment("reader_c") }
  Text("C: $value", modifier = Modifier.testTag("reader_c"))
}

@Composable fun DualReader(a: Int, b: Int) {
  SideEffect { GroundTruthCounters.increment("dual_reader") }
  Text("A+B: ${a + b}", modifier = Modifier.testTag("dual_reader"))
}

@Composable fun ThemeReader() {
  SideEffect { GroundTruthCounters.increment("theme_reader") }
  val isDark = LocalThemeMode.current
  Text("Theme: ${if (isDark) "Dark" else "Light"}", modifier = Modifier.testTag("theme_reader"))
}

@Composable fun AllReader(a: Int, b: Int, c: Int) {
  SideEffect { GroundTruthCounters.increment("all_reader") }
  Text("All: ${a + b + c}", modifier = Modifier.testTag("all_reader"))
}

@Composable fun IncAButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("inc_a_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("inc_a_btn")) { Text("Inc A") }
}

@Composable fun IncBButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("inc_b_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("inc_b_btn")) { Text("Inc B") }
}

@Composable fun IncCButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("inc_c_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("inc_c_btn")) { Text("Inc C") }
}

@Composable fun ToggleThemeButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("toggle_theme_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("toggle_theme_btn")) { Text("Toggle Theme") }
}
