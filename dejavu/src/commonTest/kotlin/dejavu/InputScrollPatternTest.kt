package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
class InputScrollPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun typeCharacter_displayRecomposesOnce() = runComposeUiTest {
        setContent { DejavuTestContent { InputScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("type_char_btn").performClick()
        waitForIdle()

        onNodeWithTag("text_display").assertRecompositions(exactly = 1)
    }

    @Test
    fun typeCharacter_unrelatedStable() = runComposeUiTest {
        setContent { DejavuTestContent { InputScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("type_char_btn").performClick()
        waitForIdle()

        onNodeWithTag("text_unrelated").assertStable()
    }

    @Test
    fun rapidTyping_eachKeystrokeTracked() = runComposeUiTest {
        setContent { DejavuTestContent { InputScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        repeat(5) {
            onNodeWithTag("type_char_btn").performClick()
            waitForIdle()
        }

        onNodeWithTag("text_display").assertRecompositions(exactly = 5)
    }

    @Test
    fun produceState_recomposesOnTriggerChange() = runComposeUiTest {
        setContent { DejavuTestContent { InputScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_source_btn").performClick()
        waitForIdle()

        onNodeWithTag("produced_value").assertRecompositions(atLeast = 1)
    }

    @Test
    fun snapshotFlowChange_recomposesReader() = runComposeUiTest {
        setContent { DejavuTestContent { InputScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_source_btn").performClick()
        waitForIdle()

        onNodeWithTag("flow_reader").assertRecompositions(atLeast = 1)
    }

    @Test
    fun snapshotFlowSameValue_readerStable() = runComposeUiTest {
        setContent { DejavuTestContent { InputScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("same_source_btn").performClick()
        waitForIdle()

        onNodeWithTag("flow_reader").assertStable()
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun InputScreen() {
    var text by remember { mutableStateOf("") }
    var source by remember { mutableIntStateOf(0) }
    Column {
        TextDisplay(text)
        TextUnrelated()
        ProducedValueReader(source)
        FlowReaderComposable(source)
        BasicText("TypeChar", Modifier.testTag("type_char_btn").clickable { text += "a" })
        BasicText("ChangeSource", Modifier.testTag("change_source_btn").clickable { source++ })
        BasicText("SameSource", Modifier.testTag("same_source_btn").clickable { source = source })
    }
}

@Composable
private fun TextDisplay(text: String) {
    BasicText("Text: $text", Modifier.testTag("text_display"))
}

@Composable
private fun TextUnrelated() {
    BasicText("Unrelated", Modifier.testTag("text_unrelated"))
}

@Composable
private fun ProducedValueReader(trigger: Int) {
    val produced by produceState(initialValue = 0, trigger) { value = trigger * 10 }
    BasicText("Produced: $produced", Modifier.testTag("produced_value"))
}

@Composable
private fun FlowReaderComposable(source: Int) {
    val sourceState = remember { mutableIntStateOf(source) }
    sourceState.intValue = source
    var flowValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        snapshotFlow { sourceState.intValue }.collect { flowValue = it }
    }
    BasicText("Flow: $flowValue", Modifier.testTag("flow_reader"))
}
