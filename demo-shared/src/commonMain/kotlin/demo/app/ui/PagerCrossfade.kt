package demo.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import demo.app.GroundTruthCounters
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PagerCrossfadeScreen() {
  SideEffect { GroundTruthCounters.increment("pager_crossfade_root") }
  val pagerState = rememberPagerState(pageCount = { 5 })
  var crossfadeTarget by remember { mutableIntStateOf(0) }
  val coroutineScope = rememberCoroutineScope()

  Column(modifier = Modifier.testTag("pager_crossfade_root")) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
    ) { page ->
      PageContent(page)
    }

    PagerIndicator(pagerState.currentPage)

    Crossfade(targetState = crossfadeTarget, label = "crossfade") { target ->
      when (target) {
        0 -> CrossfadeVariantA()
        1 -> CrossfadeVariantB()
        else -> CrossfadeVariantC()
      }
    }

    CrossfadeLabel(crossfadeTarget)
    StaticPagerSibling()

    Row {
      NextPageButton {
        coroutineScope.launch {
          val next = (pagerState.currentPage + 1).coerceAtMost(4)
          pagerState.animateScrollToPage(next)
        }
      }
      CycleCrossfadeButton { crossfadeTarget = (crossfadeTarget + 1) % 3 }
      ResetCrossfadeButton { crossfadeTarget = 0 }
    }
  }
}

@Composable
fun PageContent(page: Int) {
  SideEffect { GroundTruthCounters.increment("page_content_$page") }
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxWidth()
      .height(100.dp)
      .testTag("page_content_$page")
  ) {
    Text("Page $page")
  }
}

@Composable
fun PagerIndicator(currentPage: Int) {
  SideEffect { GroundTruthCounters.increment("pager_indicator") }
  Text("Page: $currentPage", modifier = Modifier.testTag("pager_indicator"))
}

@Composable
fun CrossfadeVariantA() {
  SideEffect { GroundTruthCounters.increment("crossfade_a") }
  Text("Variant A", modifier = Modifier.testTag("crossfade_a"))
}

@Composable
fun CrossfadeVariantB() {
  SideEffect { GroundTruthCounters.increment("crossfade_b") }
  Text("Variant B", modifier = Modifier.testTag("crossfade_b"))
}

@Composable
fun CrossfadeVariantC() {
  SideEffect { GroundTruthCounters.increment("crossfade_c") }
  Text("Variant C", modifier = Modifier.testTag("crossfade_c"))
}

@Composable
fun CrossfadeLabel(target: Int) {
  SideEffect { GroundTruthCounters.increment("crossfade_label") }
  Text("Crossfade: $target", modifier = Modifier.testTag("crossfade_label"))
}

@Composable
fun StaticPagerSibling() {
  SideEffect { GroundTruthCounters.increment("pager_sibling") }
  Text("Static sibling", modifier = Modifier.testTag("pager_sibling"))
}

@Composable
fun NextPageButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("next_page_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("next_page_btn")) {
    Text("Next Page")
  }
}

@Composable
fun CycleCrossfadeButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("cycle_crossfade_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("cycle_crossfade_btn")) {
    Text("Cycle")
  }
}

@Composable
fun ResetCrossfadeButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("reset_crossfade_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("reset_crossfade_btn")) {
    Text("Reset")
  }
}
