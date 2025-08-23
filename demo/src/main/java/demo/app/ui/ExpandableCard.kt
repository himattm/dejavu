package demo.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import demo.app.GroundTruthCounters

private val cardData = listOf(
  "Getting Started" to "Welcome to the accordion demo. This section covers basics.",
  "Configuration" to "Configure your settings here. Adjust preferences as needed.",
  "Advanced Usage" to "Learn about advanced features and customization options.",
  "Troubleshooting" to "Common issues and their solutions are listed here.",
  "FAQ" to "Frequently asked questions answered in this section."
)

@Composable
fun AccordionListScreen() {
  SideEffect { GroundTruthCounters.increment("accordion_list_root") }
  val expandedIndexState = remember { mutableStateOf<Int?>(null) }
  val expandedIndex = expandedIndexState.value
  val onCardClick = remember<(Int) -> Unit> {
    { index ->
      val current = expandedIndexState.value
      expandedIndexState.value = if (current == index) null else index
    }
  }
  Column(modifier = Modifier.testTag("accordion_list_root")) {
    AccordionTitle()
    AccordionList(
      expandedIndex = expandedIndex,
      onCardClick = onCardClick,
    )
  }
}

@Composable
fun AccordionTitle() {
  SideEffect { GroundTruthCounters.increment("accordion_title") }
  Text(
    "Accordion List",
    style = MaterialTheme.typography.headlineMedium,
    modifier = Modifier
      .testTag("accordion_title")
      .padding(16.dp)
  )
}

@Composable
fun AccordionList(expandedIndex: Int?, onCardClick: (Int) -> Unit) {
  SideEffect { GroundTruthCounters.increment("accordion_list") }
  LazyColumn(modifier = Modifier.testTag("accordion_list")) {
    itemsIndexed(cardData) { index, (title, content) ->
      ExpandableCard(
        index = index,
        title = title,
        content = content,
        isExpanded = expandedIndex == index,
        onCardClick = onCardClick,
      )
    }
  }
}

@Composable
fun ExpandableCard(
  index: Int,
  title: String,
  content: String,
  isExpanded: Boolean,
  onCardClick: (Int) -> Unit,
) {
  SideEffect { GroundTruthCounters.increment("card_$index") }
  Column(modifier = Modifier.testTag("card_$index")) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
      Column(modifier = Modifier.animateContentSize()) {
        CardHeader(index = index, title = title, isExpanded = isExpanded, onCardClick = onCardClick)
        if (isExpanded) {
          CardContent(index = index, content = content)
        }
      }
    }
  }
}

@Composable
fun CardHeader(index: Int, title: String, isExpanded: Boolean, onCardClick: (Int) -> Unit) {
  SideEffect { GroundTruthCounters.increment("card_header_$index") }
  Row(
    modifier = Modifier
      .testTag("card_header_$index")
      .fillMaxWidth()
      .clickable { onCardClick(index) }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.weight(1f))
    Text(if (isExpanded) "\u25B2" else "\u25BC")
  }
}

@Composable
fun CardContent(index: Int, content: String) {
  SideEffect { GroundTruthCounters.increment("card_content_$index") }
  Text(
    content,
    modifier = Modifier
      .testTag("card_content_$index")
      .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
  )
}
