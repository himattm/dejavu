package demo.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import demo.app.GroundTruthCounters

private val dataSets = listOf(
  listOf(30f, 25f, 20f, 15f, 10f),
  listOf(10f, 35f, 15f, 25f, 15f),
  listOf(20f, 20f, 20f, 20f, 20f),
)

private val labels = listOf("Food", "Transport", "Entertainment", "Utilities", "Other")

private val segmentColors = listOf(
  Color(0xFFFF6384), Color(0xFF36A2EB), Color(0xFFFFCE56),
  Color(0xFF4BC0C0), Color(0xFF9966FF)
)

@Composable
fun DonutChartScreen() {
  SideEffect { GroundTruthCounters.increment("donut_chart_root") }
  var dataSetIndex by remember { mutableIntStateOf(0) }
  var selectedIndex by remember { mutableStateOf<Int?>(null) }
  val data = dataSets[dataSetIndex]

  Column(
    modifier = Modifier
      .testTag("donut_chart_root")
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    DonutChart(data = data, selectedIndex = selectedIndex)
    Spacer(modifier = Modifier.height(16.dp))
    ChartLegend(data = data, selectedIndex = selectedIndex)
    Spacer(modifier = Modifier.height(16.dp))
    ChangeDataButton { dataSetIndex = (dataSetIndex + 1) % dataSets.size }
    SelectSegmentButton(index = 0, label = "Select Food") { selectedIndex = 0 }
    SelectSegmentButton(index = 2, label = "Select Entertainment") { selectedIndex = 2 }
    ClearSelectionButton { selectedIndex = null }
  }
}

@Composable
fun DonutChart(data: List<Float>, selectedIndex: Int?) {
  SideEffect { GroundTruthCounters.increment("donut_chart") }
  val total = data.sum()
  val sweepAngles = data.mapIndexed { index, value ->
    animateFloatAsState(
      targetValue = (value / total) * 360f,
      label = "sweep_$index"
    )
  }

  Box(modifier = Modifier.testTag("donut_chart")) {
    Canvas(modifier = Modifier.size(200.dp)) {
    val strokeWidth = 40f
    val selectedStrokeWidth = 52f
    val canvasSize = size.minDimension
    val radius = (canvasSize - selectedStrokeWidth) / 2f
    val topLeft = Offset(
      (size.width - radius * 2) / 2f,
      (size.height - radius * 2) / 2f
    )
    val arcSize = Size(radius * 2, radius * 2)

    var startAngle = -90f
    sweepAngles.forEachIndexed { index, animatedSweep ->
      val sweep = animatedSweep.value
      val isSelected = selectedIndex == index
      drawArc(
        color = segmentColors[index],
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = if (isSelected) selectedStrokeWidth else strokeWidth)
      )
      startAngle += sweep
    }
    }
  }
}

@Composable
fun ChartLegend(data: List<Float>, selectedIndex: Int?) {
  SideEffect { GroundTruthCounters.increment("chart_legend") }
  val total = data.sum()
  Column(modifier = Modifier.testTag("chart_legend")) {
    data.forEachIndexed { index, value ->
      LegendItem(
        index = index,
        label = labels[index],
        color = segmentColors[index],
        percentage = (value / total * 100f),
        isSelected = selectedIndex == index,
      )
    }
  }
}

@Composable
fun LegendItem(
  index: Int,
  label: String,
  color: Color,
  percentage: Float,
  isSelected: Boolean,
) {
  SideEffect { GroundTruthCounters.increment("legend_item_$index") }
  Row(
    modifier = Modifier
      .testTag("legend_item_$index")
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(12.dp)
        .background(color, CircleShape)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "$label: ${"%.1f".format(percentage)}%",
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      fontSize = if (isSelected) 16.sp else 14.sp,
    )
  }
}

@Composable
fun ChangeDataButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("change_data_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("change_data_btn")) {
    Text("Change Data")
  }
}

@Composable
fun SelectSegmentButton(index: Int, label: String, onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("select_segment_${index}_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("select_segment_${index}_btn")) {
    Text(label)
  }
}

@Composable
fun ClearSelectionButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("clear_selection_btn") }
  Button(onClick = onClick, modifier = Modifier.testTag("clear_selection_btn")) {
    Text("Clear Selection")
  }
}
