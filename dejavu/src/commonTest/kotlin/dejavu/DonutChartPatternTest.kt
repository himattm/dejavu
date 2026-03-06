package dejavu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Cross-platform port of Android DonutChartTest.
 * Canvas-based donut chart with legend items and segment selection.
 */
@OptIn(ExperimentalTestApi::class)
class DonutChartPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun chart_recomposes_on_data_change() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_data_btn").performClick()
        waitForIdle()

        onNodeWithTag("donut_chart").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chart_legend_recomposes_on_data_change() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_data_btn").performClick()
        waitForIdle()

        onNodeWithTag("chart_legend").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chart_selected_legend_item_recomposes() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("select_segment_0_btn").performClick()
        waitForIdle()

        onNodeWithTag("legend_item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chart_unselected_legend_items_stable() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("select_segment_0_btn").performClick()
        waitForIdle()

        onNodeWithTag("legend_item_1").assertRecompositions(atMost = 1)
        onNodeWithTag("legend_item_2").assertRecompositions(atMost = 1)
        onNodeWithTag("legend_item_3").assertRecompositions(atMost = 1)
        onNodeWithTag("legend_item_4").assertRecompositions(atMost = 1)
    }

    @Test
    fun chart_clear_selection_recomposes_selected() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()

        onNodeWithTag("select_segment_0_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("clear_selection_btn").performClick()
        waitForIdle()

        onNodeWithTag("legend_item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chart_change_data_button_stable() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_data_btn").performClick()
        waitForIdle()

        onNodeWithTag("change_data_btn").assertStable()
    }

    @Test
    fun chart_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()

        onNodeWithTag("donut_chart_root").assertStable()
        onNodeWithTag("donut_chart").assertStable()
        onNodeWithTag("chart_legend").assertStable()
        // LegendItem and SelectSegmentButton are multi-instance (share qualified-name
        // counter). Per-instance stability is tested via resetRecompositionCounts in
        // chart_unselected_legend_items_stable.
    }
}

// ── Composables ──────────────────────────────────────────────────

private val chartDataSets = listOf(
    listOf(30f, 25f, 20f, 15f, 10f),
    listOf(10f, 35f, 15f, 25f, 15f),
    listOf(20f, 20f, 20f, 20f, 20f),
)

private val chartLabels = listOf("Food", "Transport", "Entertainment", "Utilities", "Other")

private val chartColors = listOf(
    Color(0xFFFF6384), Color(0xFF36A2EB), Color(0xFFFFCE56),
    Color(0xFF4BC0C0), Color(0xFF9966FF)
)

@Composable
private fun DonutChartScreen() {
    var dataSetIndex by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val data = chartDataSets[dataSetIndex]

    Column(Modifier.testTag("donut_chart_root")) {
        DonutChart(data = data, selectedIndex = selectedIndex)
        Spacer(modifier = Modifier.height(16.dp))
        ChartLegend(data = data, selectedIndex = selectedIndex)
        Spacer(modifier = Modifier.height(16.dp))
        ChangeDataButton { dataSetIndex = (dataSetIndex + 1) % chartDataSets.size }
        SelectSegmentButton(index = 0) { selectedIndex = 0 }
        SelectSegmentButton(index = 2) { selectedIndex = 2 }
        ClearSelectionButton { selectedIndex = null }
    }
}

@Composable
private fun DonutChart(data: List<Float>, selectedIndex: Int?) {
    val total = data.sum()
    val sweepAngles = data.mapIndexed { index, value ->
        animateFloatAsState(
            targetValue = (value / total) * 360f,
            label = "sweep_$index"
        )
    }

    Canvas(modifier = Modifier.testTag("donut_chart").size(200.dp)) {
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
                color = chartColors[index],
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

@Composable
private fun ChartLegend(data: List<Float>, selectedIndex: Int?) {
    val total = data.sum()
    Column(Modifier.testTag("chart_legend")) {
        data.forEachIndexed { index, value ->
            LegendItem(
                index = index,
                label = chartLabels[index],
                percentage = (value / total * 100f),
                isSelected = selectedIndex == index,
            )
        }
    }
}

@Composable
private fun LegendItem(
    index: Int,
    label: String,
    percentage: Float,
    isSelected: Boolean,
) {
    Row(
        modifier = Modifier
            .testTag("legend_item_$index")
            .fillMaxWidth(),
    ) {
        BasicText(
            text = "$label: ${percentage.toInt()}%" + if (isSelected) " *" else "",
        )
    }
}

@Composable
private fun ChangeDataButton(onClick: () -> Unit) {
    BasicText("Change Data", Modifier.testTag("change_data_btn").clickable(onClick = onClick))
}

@Composable
private fun SelectSegmentButton(index: Int, onClick: () -> Unit) {
    BasicText(
        "Select $index",
        Modifier.testTag("select_segment_${index}_btn").clickable(onClick = onClick)
    )
}

@Composable
private fun ClearSelectionButton(onClick: () -> Unit) {
    BasicText("Clear Selection", Modifier.testTag("clear_selection_btn").clickable(onClick = onClick))
}
