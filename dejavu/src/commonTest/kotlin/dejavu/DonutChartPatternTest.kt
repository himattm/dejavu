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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cross-platform port of Android DonutChartTest.
 * Canvas-based donut chart with legend items and segment selection.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect` (the runtime
 * runs the effect after every successful composition, so it is the real composition count). The
 * donut animates its segment sweeps, but `animateFloatAsState`'s value is read inside the `Canvas`
 * *draw* scope — animation frames drive redraws, not recompositions of `DonutChart` — so the chart
 * settles under `waitForIdle()` and its recomposition count is deterministic.
 *
 * Instance classification:
 * - **Single-instance** nodes (`donut_chart_root`, `donut_chart`, `chart_legend`, `change_data_btn`,
 *   `clear_selection_btn`) and the two distinct `SelectSegmentButton` call sites
 *   (`select_segment_0_btn`, `select_segment_2_btn`) have unique composer keys, so the public per-tag
 *   API resolves their exact count on every platform → `exactly = GroundTruth.delta(tag)`.
 * - The five `LegendItem`s are emitted from a keyless `forEachIndexed` loop, so they share one
 *   composer key and their per-*instance* counts only resolve on Android. On the common targets the
 *   public per-tag count falls back to the shared *function-level* sum, so these assert the
 *   function-level count — `DejavuTracer.getRecompositionCount("dejavu.LegendItem")` ==
 *   `GroundTruth.delta("LegendItem")`. Per-instance legend isolation is covered on Android by the
 *   demo instrumented tests.
 */
@OptIn(ExperimentalTestApi::class)
class DonutChartPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun chart_recomposes_on_data_change() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("change_data_btn").performClick()
        waitForIdle()

        // Changing the data set hands DonutChart a new `data` list, recomposing it once.
        onNodeWithTag("donut_chart")
            .assertRecompositions(exactly = GroundTruth.delta("donut_chart"))
        assertEquals(1, GroundTruth.delta("donut_chart"), "chart recomposes once when its data changes")
    }

    @Test
    fun chart_legend_recomposes_on_data_change() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("change_data_btn").performClick()
        waitForIdle()

        // Changing the data set hands ChartLegend a new `data` list, recomposing it once.
        onNodeWithTag("chart_legend")
            .assertRecompositions(exactly = GroundTruth.delta("chart_legend"))
        assertEquals(1, GroundTruth.delta("chart_legend"), "legend recomposes once when its data changes")
    }

    @Test
    fun chart_selected_legend_item_recomposes() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("select_segment_0_btn").performClick()
        waitForIdle()

        // Selecting segment 0 flips legend_item_0's isSelected (false→true); items 1-4 keep their
        // params so Compose skips them. The legend items come from one call site, so on the common
        // targets their counts aggregate at the function level — exactly one item composed.
        assertEquals(
            GroundTruth.delta("LegendItem"),
            DejavuTracer.getRecompositionCount("dejavu.LegendItem"),
            "tracer LegendItem count should equal SideEffect ground truth",
        )
        assertEquals(1, GroundTruth.delta("LegendItem"), "only the newly-selected legend item recomposes")
    }

    @Test
    fun chart_unselected_legend_items_stable() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("select_segment_0_btn").performClick()
        waitForIdle()

        // Only legend_item_0 changes (isSelected false→true); the other four keep their params and
        // are skipped. The five items share one call site, so on the common targets their counts
        // aggregate at the function level: a total of exactly 1 proves items 1-4 stayed stable (any
        // unselected item recomposing would push the function-level total above 1). Per-instance
        // isolation for the unchanged items is verified on Android in the demo instrumented tests.
        assertEquals(
            GroundTruth.delta("LegendItem"),
            DejavuTracer.getRecompositionCount("dejavu.LegendItem"),
            "tracer LegendItem count should equal SideEffect ground truth",
        )
        assertEquals(
            1,
            GroundTruth.delta("LegendItem"),
            "only legend_item_0 recomposes; the four unselected items stay stable",
        )
    }

    @Test
    fun chart_clear_selection_recomposes_selected() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()

        onNodeWithTag("select_segment_0_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("clear_selection_btn").performClick()
        waitForIdle()

        // Clearing selection flips legend_item_0's isSelected (true→false); items 1-4 keep their
        // params and are skipped. Function-level: exactly one legend item composed.
        assertEquals(
            GroundTruth.delta("LegendItem"),
            DejavuTracer.getRecompositionCount("dejavu.LegendItem"),
            "tracer LegendItem count should equal SideEffect ground truth",
        )
        assertEquals(1, GroundTruth.delta("LegendItem"), "clearing selection recomposes only the previously-selected item")
    }

    @Test
    fun chart_change_data_button_stable() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("change_data_btn").performClick()
        waitForIdle()

        onNodeWithTag("change_data_btn")
            .assertRecompositions(exactly = GroundTruth.delta("change_data_btn"))
        assertEquals(0, GroundTruth.delta("change_data_btn"), "the clicked button's own composition does not change")
        onNodeWithTag("change_data_btn").assertStable()
    }

    @Test
    fun chart_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { DonutChartScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No interaction occurred, so every node must be stable. Single-instance nodes assert
        // exactly against their per-tag delta; the loop-emitted legend items assert at the
        // function level (per-instance counts are Android-only — see demo instrumented tests).
        onNodeWithTag("donut_chart_root")
            .assertRecompositions(exactly = GroundTruth.delta("donut_chart_root"))
        onNodeWithTag("donut_chart")
            .assertRecompositions(exactly = GroundTruth.delta("donut_chart"))
        onNodeWithTag("chart_legend")
            .assertRecompositions(exactly = GroundTruth.delta("chart_legend"))
        assertEquals(0, GroundTruth.delta("donut_chart_root"))
        assertEquals(0, GroundTruth.delta("donut_chart"))
        assertEquals(0, GroundTruth.delta("chart_legend"))

        assertEquals(
            GroundTruth.delta("LegendItem"),
            DejavuTracer.getRecompositionCount("dejavu.LegendItem"),
            "tracer LegendItem count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("LegendItem"), "no legend item composes without interaction")

        onNodeWithTag("donut_chart_root").assertStable()
        onNodeWithTag("donut_chart").assertStable()
        onNodeWithTag("chart_legend").assertStable()
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

    SideEffect { GroundTruth.record("donut_chart_root") }
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
    SideEffect { GroundTruth.record("donut_chart") }
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
    SideEffect { GroundTruth.record("chart_legend") }
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
    // One call site (keyless forEachIndexed) emits every legend item, so the tracer aggregates them
    // under the shared compile-time key on non-Android. Record at the function level to match the
    // function-level assertions (per-instance counts are Android-only — see demo instrumented tests).
    SideEffect { GroundTruth.record("LegendItem") }
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
    SideEffect { GroundTruth.record("change_data_btn") }
    BasicText("Change Data", Modifier.testTag("change_data_btn").clickable(onClick = onClick))
}

@Composable
private fun SelectSegmentButton(index: Int, onClick: () -> Unit) {
    SideEffect { GroundTruth.record("select_segment_${index}_btn") }
    BasicText(
        "Select $index",
        Modifier.testTag("select_segment_${index}_btn").clickable(onClick = onClick)
    )
}

@Composable
private fun ClearSelectionButton(onClick: () -> Unit) {
    SideEffect { GroundTruth.record("clear_selection_btn") }
    BasicText("Clear Selection", Modifier.testTag("clear_selection_btn").clickable(onClick = onClick))
}
