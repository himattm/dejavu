package demo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import demo.app.GroundTruthCounters
import kotlinx.coroutines.launch

@Composable
fun CollapsingHeaderScreen() {
  SideEffect { GroundTruthCounters.increment("collapsing_header_root") }
  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()
  val collapseFraction = (scrollState.value / 400f).coerceIn(0f, 1f)

  Box(modifier = Modifier
    .testTag("collapsing_header_root")
    .fillMaxSize()
  ) {
    ScrollContent(scrollState = scrollState, headerHeight = lerp(200f, 56f, collapseFraction))

    CollapsingHeader(collapseFraction = collapseFraction)

    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(8.dp),
    ) {
      ScrollToTopButton { coroutineScope.launch { scrollState.animateScrollTo(0) } }
      Spacer(modifier = Modifier.weight(1f))
      ScrollToBottomButton { coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) } }
    }
  }
}

@Composable
fun CollapsingHeader(collapseFraction: Float) {
  SideEffect { GroundTruthCounters.increment("collapsing_header") }
  val headerHeight = lerp(200f, 56f, collapseFraction)
  Box(
    modifier = Modifier
      .testTag("collapsing_header")
      .fillMaxWidth()
      .height(headerHeight.dp)
      .background(Color(0xFF6200EE))
  ) {
    HeaderImage(alpha = lerp(1f, 0f, collapseFraction))
    HeaderTitle(collapseFraction = collapseFraction)
  }
}

@Composable
fun HeaderImage(alpha: Float) {
  SideEffect { GroundTruthCounters.increment("header_image") }
  Box(
    modifier = Modifier
      .testTag("header_image")
      .fillMaxSize()
      .alpha(alpha)
      .background(Color(0xFF3700B3))
  )
}

@Composable
fun HeaderTitle(collapseFraction: Float) {
  SideEffect { GroundTruthCounters.increment("header_title") }
  val fontSize = lerp(24f, 16f, collapseFraction)
  Text(
    text = "Collapsing Header",
    color = Color.White,
    fontSize = fontSize.sp,
    modifier = Modifier
      .testTag("header_title")
      .padding(16.dp)
  )
}

@Composable
fun ScrollContent(scrollState: androidx.compose.foundation.ScrollState, headerHeight: Float) {
  SideEffect { GroundTruthCounters.increment("scroll_content") }
  Column(
    modifier = Modifier
      .testTag("scroll_content")
      .verticalScroll(scrollState)
      .padding(top = headerHeight.dp)
  ) {
    for (i in 0 until 30) {
      ScrollContentItem(index = i)
    }
  }
}

@Composable
fun ScrollContentItem(index: Int) {
  SideEffect { GroundTruthCounters.increment("scroll_content_item_$index") }
  Text(
    text = "Item $index",
    modifier = Modifier
      .testTag("scroll_content_item_$index")
      .fillMaxWidth()
      .padding(16.dp),
    fontSize = 16.sp,
  )
}

@Composable
fun ScrollToTopButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("scroll_to_top_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("scroll_to_top_btn")) {
    Text("Scroll Top")
  }
}

@Composable
fun ScrollToBottomButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("scroll_to_bottom_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("scroll_to_bottom_btn")) {
    Text("Scroll Bottom")
  }
}
