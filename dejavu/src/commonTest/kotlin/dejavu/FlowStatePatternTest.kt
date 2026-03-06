package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FlowStatePatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun flowEmission_recomposesCollector() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("flow_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("flow_counter").assertRecompositions(exactly = 1)
    }

    @Test
    fun multipleFlowEmissions_eachTracked() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        repeat(3) {
            onNodeWithTag("flow_inc_btn").performClick()
            waitForIdle()
        }

        onNodeWithTag("flow_counter").assertRecompositions(exactly = 3)
    }

    @Test
    fun flowEmission_doesNotAffectUnrelatedComposables() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("flow_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("same_value_reader").assertStable()
    }

    @Test
    fun stateCoalescing_multipleWritesInOneFrame() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("flow_inc_three_btn").performClick()
        waitForIdle()

        onNodeWithTag("flow_counter").assertRecompositions(exactly = 1)
    }

    @Test
    fun multipleStateWritesInOneCallback_coalescesToOneRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("coalesce_btn").performClick()
        waitForIdle()

        onNodeWithTag("batch_reader_a").assertRecompositions(exactly = 1)
        onNodeWithTag("batch_reader_b").assertRecompositions(exactly = 1)
    }

    @Test
    fun sameValueWrite_doesNotCauseRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("same_value_write_btn").performClick()
        waitForIdle()

        onNodeWithTag("same_value_reader").assertStable()
    }

    @Test
    fun realValueWrite_doesCauseRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("real_value_write_btn").performClick()
        waitForIdle()

        onNodeWithTag("same_value_reader").assertRecompositions(exactly = 1)
    }

    @Test
    fun addToList_recomposesListReader() = runComposeUiTest {
        setContent { DejavuTestContent { FlowStateScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("add_item_btn").performClick()
        waitForIdle()

        onNodeWithTag("list_size_reader").assertRecompositions(atLeast = 1)
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
    BasicText("Flow: $value", Modifier.testTag("flow_counter"))
}

@Composable
private fun SameValueReader(value: Int) {
    BasicText("Same: $value", Modifier.testTag("same_value_reader"))
}

@Composable
private fun BatchReaderA(value: Int) {
    BasicText("BatchA: $value", Modifier.testTag("batch_reader_a"))
}

@Composable
private fun BatchReaderB(value: Int) {
    BasicText("BatchB: $value", Modifier.testTag("batch_reader_b"))
}

@Composable
private fun ListSizeReader(size: Int) {
    BasicText("List: $size", Modifier.testTag("list_size_reader"))
}
