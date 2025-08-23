package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldSlotsScreen() {
  SideEffect { GroundTruthCounters.increment("scaffold_root") }
  var topBarTitle by remember { mutableStateOf("Title") }
  var bottomCount by remember { mutableIntStateOf(0) }
  var contentValue by remember { mutableIntStateOf(0) }
  var fabClickCount by remember { mutableIntStateOf(0) }

  Scaffold(
    modifier = Modifier.testTag("scaffold_root"),
    topBar = { ScaffoldTopBar(topBarTitle) },
    bottomBar = { ScaffoldBottomBar(bottomCount) },
    floatingActionButton = { ScaffoldFab { fabClickCount++ } }
  ) { innerPadding ->
    ScaffoldContent(
      contentValue = contentValue,
      fabClickCount = fabClickCount,
      onChangeTopBar = { topBarTitle = if (topBarTitle == "Title") "Changed" else "Title" },
      onChangeBottom = { bottomCount++ },
      onChangeContent = { contentValue++ },
      modifier = Modifier.padding(innerPadding)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldTopBar(title: String) {
  SideEffect { GroundTruthCounters.increment("top_bar_wrapper") }
  TopAppBar(
    modifier = Modifier.testTag("top_bar_wrapper"),
    title = { TopBarTitle(title) }
  )
}

@Composable
fun TopBarTitle(title: String) {
  SideEffect { GroundTruthCounters.increment("scaffold_top_bar") }
  Text(title, modifier = Modifier.testTag("scaffold_top_bar"))
}

@Composable
fun ScaffoldBottomBar(count: Int) {
  SideEffect { GroundTruthCounters.increment("bottom_bar_wrapper") }
  BottomAppBar(modifier = Modifier.testTag("bottom_bar_wrapper")) {
    BottomBarContent(count)
  }
}

@Composable
fun BottomBarContent(count: Int) {
  SideEffect { GroundTruthCounters.increment("scaffold_bottom_bar") }
  Text("Bottom: $count", modifier = Modifier.testTag("scaffold_bottom_bar"))
}

@Composable
fun ScaffoldFab(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("scaffold_fab") }
  FloatingActionButton(
    onClick = onClick,
    modifier = Modifier.testTag("scaffold_fab")
  ) {
    Text("+")
  }
}

@Composable
fun ScaffoldContent(
  contentValue: Int,
  fabClickCount: Int,
  onChangeTopBar: () -> Unit,
  onChangeBottom: () -> Unit,
  onChangeContent: () -> Unit,
  modifier: Modifier = Modifier
) {
  SideEffect { GroundTruthCounters.increment("scaffold_content") }
  Column(modifier = modifier.testTag("scaffold_content")) {
    ContentBody(contentValue)
    ContentStatic()
    FabClickLabel(fabClickCount)
    ChangeTopBarButton(onChangeTopBar)
    ChangeBottomButton(onChangeBottom)
    ChangeContentButton(onChangeContent)
  }
}

@Composable
fun ContentBody(value: Int) {
  SideEffect { GroundTruthCounters.increment("content_body") }
  Text("Content: $value", modifier = Modifier.testTag("content_body"))
}

@Composable
fun ContentStatic() {
  SideEffect { GroundTruthCounters.increment("content_static") }
  Text("Static content", modifier = Modifier.testTag("content_static"))
}

@Composable
fun FabClickLabel(count: Int) {
  SideEffect { GroundTruthCounters.increment("fab_click_label") }
  Text("FAB clicks: $count", modifier = Modifier.testTag("fab_click_label"))
}

@Composable
fun ChangeTopBarButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_top_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_top_btn")) {
    Text("Change Top Bar")
  }
}

@Composable
fun ChangeBottomButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_bottom_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_bottom_btn")) {
    Text("Change Bottom")
  }
}

@Composable
fun ChangeContentButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_content_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_content_btn")) {
    Text("Change Content")
  }
}
