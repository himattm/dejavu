package demo.app.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import demo.app.GroundTruthCounters

@Composable
fun RatingBarScreen() {
  SideEffect { GroundTruthCounters.increment("rating_bar_root") }
  var rating by remember { mutableFloatStateOf(0f) }
  Column(modifier = Modifier.testTag("rating_bar_root").padding(16.dp)) {
    StaticRatingLabel()
    RatingBar(rating = rating, onRatingChange = { rating = it })
    RatingDisplay(rating = rating)
    SetRatingButton(label = "Set 1", tag = "set_rating_1_btn") { rating = 1f }
    SetRatingButton(label = "Set 3", tag = "set_rating_3_btn") { rating = 3f }
    SetRatingButton(label = "Set 5", tag = "set_rating_5_btn") { rating = 5f }
  }
}

@Composable
fun StaticRatingLabel() {
  SideEffect { GroundTruthCounters.increment("static_rating_label") }
  Text(
    "Rate this item",
    style = MaterialTheme.typography.headlineMedium,
    modifier = Modifier.testTag("static_rating_label")
  )
}

@Composable
fun RatingBar(rating: Float, onRatingChange: (Float) -> Unit) {
  SideEffect { GroundTruthCounters.increment("rating_bar") }
  Row(modifier = Modifier.testTag("rating_bar")) {
    for (i in 0 until 5) {
      Star(
        index = i,
        isFilled = i < rating,
        onClick = { onRatingChange((i + 1).toFloat()) }
      )
    }
  }
}

@Composable
fun Star(index: Int, isFilled: Boolean, onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("star_$index") }
  Text(
    text = if (isFilled) "\u2605" else "\u2606",
    fontSize = 40.sp,
    color = if (isFilled) Color(0xFFFFD700) else Color.Gray,
    modifier = Modifier
      .testTag("star_$index")
      .size(48.dp)
      .pointerInput(Unit) {
        detectTapGestures { onClick() }
      }
  )
}

@Composable
fun RatingDisplay(rating: Float) {
  SideEffect { GroundTruthCounters.increment("rating_display") }
  Text(
    "Rating: $rating / 5.0",
    modifier = Modifier.testTag("rating_display").padding(top = 8.dp)
  )
}

@Composable
fun SetRatingButton(label: String, tag: String, onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment(tag) }
  Button(
    onClick = onClick,
    modifier = Modifier.testTag(tag).padding(top = 4.dp)
  ) {
    Text(label)
  }
}
