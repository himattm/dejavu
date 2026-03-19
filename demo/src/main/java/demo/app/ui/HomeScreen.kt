package demo.app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import demo.app.AdvancedPatternsActivity
import demo.app.AnimationStressActivity
import demo.app.ChipFilterActivity
import demo.app.CollapsingHeaderActivity
import demo.app.CounterActivity
import demo.app.DeepNestingStressActivity
import demo.app.DialogPopupActivity
import demo.app.DonutChartActivity
import demo.app.ExpandableCardActivity
import demo.app.FlowStateActivity
import demo.app.InputScrollActivity
import demo.app.KeyIdentityActivity
import demo.app.LazyListStressActivity
import demo.app.LazyVariantsActivity
import demo.app.PagerCrossfadeActivity
import demo.app.ProductActivity
import demo.app.MissingKeyListActivity
import demo.app.ReorderListActivity
import demo.app.ScaffoldSlotsActivity
import demo.app.SharedStateStressActivity
import demo.app.StarRatingActivity
import demo.app.SubcomposeActivity
import demo.app.SwipeListActivity
import demo.app.ToggleMorphActivity

private data class DemoEntry(val title: String, val activity: Class<*>)

private val demos = listOf(
  DemoEntry("Counter", CounterActivity::class.java),
  DemoEntry("Product List", ProductActivity::class.java),
  DemoEntry("Lazy List Stress", LazyListStressActivity::class.java),
  DemoEntry("Animation Stress", AnimationStressActivity::class.java),
  DemoEntry("Deep Nesting Stress", DeepNestingStressActivity::class.java),
  DemoEntry("Shared State Stress", SharedStateStressActivity::class.java),
  DemoEntry("Flow State", FlowStateActivity::class.java),
  DemoEntry("Key Identity", KeyIdentityActivity::class.java),
  DemoEntry("Dialog & Popup", DialogPopupActivity::class.java),
  DemoEntry("Subcompose", SubcomposeActivity::class.java),
  DemoEntry("Input & Scroll", InputScrollActivity::class.java),
  DemoEntry("Lazy Variants", LazyVariantsActivity::class.java),
  DemoEntry("Pager & Crossfade", PagerCrossfadeActivity::class.java),
  DemoEntry("Advanced Patterns", AdvancedPatternsActivity::class.java),
  DemoEntry("Scaffold Slots", ScaffoldSlotsActivity::class.java),
  DemoEntry("Toggle Morph", ToggleMorphActivity::class.java),
  DemoEntry("Chip Filter", ChipFilterActivity::class.java),
  DemoEntry("Expandable Card", ExpandableCardActivity::class.java),
  DemoEntry("Star Rating", StarRatingActivity::class.java),
  DemoEntry("Donut Chart", DonutChartActivity::class.java),
  DemoEntry("Collapsing Header", CollapsingHeaderActivity::class.java),
  DemoEntry("Swipe List", SwipeListActivity::class.java),
  DemoEntry("Reorder List", ReorderListActivity::class.java),
  DemoEntry("Missing Key List", MissingKeyListActivity::class.java),
)

@Composable
fun HomeScreen() {
  val context = LocalContext.current
  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      LazyColumn {
        item {
          Text(
            text = "Dejavu Demos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
          )
        }
        items(demos) { entry ->
          Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { context.startActivity(Intent(context, entry.activity)) }
              .padding(horizontal = 16.dp, vertical = 14.dp),
          )
          HorizontalDivider()
        }
      }
    }
  }
}
