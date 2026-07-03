package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * State-observation patterns: snapshot-state reads via `collectAsState`-style consumers,
 * frame coalescing of multiple writes, same-value writes, and `mutableStateList` reads.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect` (the Compose
 * runtime runs the effect after every successful composition, so it is the real composition count).
 * All five reader composables are **single-instance** nodes reached from a single source call site
 * with a unique compile-time key, so the public per-tag API resolves their exact count on every
 * platform → `exactly = GroundTruth.delta(tag)`. The secondary deterministic `assertEquals(N, ...)`
 * pins the expected behavior (coalescing, same-value elision, etc.).
 *
 * Each test snapshots [GroundTruth.snapshotBaseline] at the same point Dejavu's counts are zeroed
 * (after the initial `waitForIdle()` / [resetRecompositionCounts]), so `delta(tag)` stays aligned
 * with the tracer's post-baseline count. After each state mutation, `waitForIdle()` settles the
 * recomposition before asserting.
 */
@OptIn(ExperimentalTestApi::class)
class FlowStatePatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun flowEmission_recomposesCollector() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("flow_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("flow_counter")
            .assertRecompositions(exactly = GroundTruth.delta("flow_counter"))
        assertEquals(1, GroundTruth.delta("flow_counter"), "one emission recomposes the collector once")
    }

    @Test
    fun multipleFlowEmissions_eachTracked() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        repeat(3) {
            onNodeWithTag("flow_inc_btn").performClick()
            waitForIdle()
        }

        onNodeWithTag("flow_counter")
            .assertRecompositions(exactly = GroundTruth.delta("flow_counter"))
        assertEquals(3, GroundTruth.delta("flow_counter"), "three separate emissions recompose the collector three times")
    }

    @Test
    fun flowEmission_doesNotAffectUnrelatedComposables() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("flow_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("same_value_reader")
            .assertRecompositions(exactly = GroundTruth.delta("same_value_reader"))
        assertEquals(0, GroundTruth.delta("same_value_reader"), "an unrelated state write must not recompose this reader")
    }

    @Test
    fun stateCoalescing_multipleWritesInOneFrame() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("flow_inc_three_btn").performClick()
        waitForIdle()

        // Three writes to the same state in one callback coalesce into a single recomposition.
        onNodeWithTag("flow_counter")
            .assertRecompositions(exactly = GroundTruth.delta("flow_counter"))
        assertEquals(1, GroundTruth.delta("flow_counter"), "writes in one frame coalesce to one recomposition")
    }

    @Test
    fun multipleStateWritesInOneCallback_coalescesToOneRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("coalesce_btn").performClick()
        waitForIdle()

        // batchA++ and batchB++ in one callback each recompose their reader exactly once.
        onNodeWithTag("batch_reader_a")
            .assertRecompositions(exactly = GroundTruth.delta("batch_reader_a"))
        onNodeWithTag("batch_reader_b")
            .assertRecompositions(exactly = GroundTruth.delta("batch_reader_b"))
        assertEquals(1, GroundTruth.delta("batch_reader_a"), "batchA reader recomposes once")
        assertEquals(1, GroundTruth.delta("batch_reader_b"), "batchB reader recomposes once")
    }

    @Test
    fun sameValueWrite_doesNotCauseRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("same_value_write_btn").performClick()
        waitForIdle()

        // Writing the same value (42 → 42) is a no-op for the snapshot system: no recomposition.
        onNodeWithTag("same_value_reader")
            .assertRecompositions(exactly = GroundTruth.delta("same_value_reader"))
        assertEquals(0, GroundTruth.delta("same_value_reader"), "same-value write must not recompose the reader")
    }

    @Test
    fun realValueWrite_doesCauseRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("real_value_write_btn").performClick()
        waitForIdle()

        // Writing a genuinely new value (42 → 99) recomposes the reader once.
        onNodeWithTag("same_value_reader")
            .assertRecompositions(exactly = GroundTruth.delta("same_value_reader"))
        assertEquals(1, GroundTruth.delta("same_value_reader"), "a real value change recomposes the reader once")
    }

    @Test
    fun addToList_recomposesListReader() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()

        // Adding an item changes items.size, recomposing the size reader exactly once.
        onNodeWithTag("list_size_reader")
            .assertRecompositions(exactly = GroundTruth.delta("list_size_reader"))
        assertEquals(1, GroundTruth.delta("list_size_reader"), "appending one item recomposes the size reader once")
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun FlowStateScreen() {
    var flowCounter by remember { mutableIntStateOf(0) }
    var sameValue by remember { mutableIntStateOf(42) }
    var batchA by remember { mutableIntStateOf(0) }
    var batchB by remember { mutableIntStateOf(0) }
    val items = remember { mutableStateListOf("a", "b", "c") }
    Column {
        FlowCounterReader(flowCounter)
        SameValueReader(sameValue)
        BatchReaderA(batchA)
        BatchReaderB(batchB)
        ListSizeReader(items.size)
        BasicText("FlowInc", Modifier.testTag("flow_inc_btn").clickable { flowCounter++ })
        BasicText("FlowInc3", Modifier.testTag("flow_inc_three_btn").clickable { flowCounter += 3 })
        BasicText("SameWrite", Modifier.testTag("same_value_write_btn").clickable { sameValue = 42 })
        BasicText("RealWrite", Modifier.testTag("real_value_write_btn").clickable { sameValue = 99 })
        BasicText("Coalesce", Modifier.testTag("coalesce_btn").clickable { batchA++; batchB++ })
        BasicText("Batch", Modifier.testTag("batch_btn").clickable { batchA++; batchB++ })
        BasicText("AddItem", Modifier.testTag("add_item_btn").clickable { items.add("new") })
    }
}

@Composable
private fun FlowCounterReader(value: Int) {
    SideEffect { GroundTruth.record("flow_counter") }
    BasicText("Flow: $value", Modifier.testTag("flow_counter"))
}

@Composable
private fun SameValueReader(value: Int) {
    SideEffect { GroundTruth.record("same_value_reader") }
    BasicText("Same: $value", Modifier.testTag("same_value_reader"))
}

@Composable
private fun BatchReaderA(value: Int) {
    SideEffect { GroundTruth.record("batch_reader_a") }
    BasicText("BatchA: $value", Modifier.testTag("batch_reader_a"))
}

@Composable
private fun BatchReaderB(value: Int) {
    SideEffect { GroundTruth.record("batch_reader_b") }
    BasicText("BatchB: $value", Modifier.testTag("batch_reader_b"))
}

@Composable
private fun ListSizeReader(size: Int) {
    SideEffect { GroundTruth.record("list_size_reader") }
    BasicText("List: $size", Modifier.testTag("list_size_reader"))
}
