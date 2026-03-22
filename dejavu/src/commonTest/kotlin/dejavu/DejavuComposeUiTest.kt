package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DejavuComposeUiTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    @Test
    fun stableComposable_hasZeroRecompositions() = runComposeUiTest {
        setContent {
            StableGreeting("Hello")
        }
        waitForIdle()

        val count = DejavuTracer.getRecompositionCount("dejavu.StableGreeting")
        assertEquals(0, count, "Stable composable should have 0 recompositions")
    }

    @Test
    fun stateChange_triggersRecomposition() = runComposeUiTest {
        setContent {
            CounterScreen()
        }
        waitForIdle()

        val initialCount = DejavuTracer.getRecompositionCount("dejavu.CounterScreen")
        assertEquals(0, initialCount, "Before click, should have 0 recompositions")

        onNodeWithTag("increment_button").performClick()
        waitForIdle()

        val afterClick = DejavuTracer.getRecompositionCount("dejavu.CounterScreen")
        assertTrue(afterClick >= 1, "After click, CounterScreen should have recomposed at least once, got $afterClick")
    }

    @Test
    fun multipleStateChanges_accumulateRecompositions() = runComposeUiTest {
        setContent {
            CounterScreen()
        }
        waitForIdle()

        repeat(3) {
            onNodeWithTag("increment_button").performClick()
            waitForIdle()
        }

        val count = DejavuTracer.getRecompositionCount("dejavu.CounterScreen")
        assertTrue(count >= 3, "After 3 clicks, should have at least 3 recompositions, got $count")
    }
}

@Composable
private fun StableGreeting(message: String) {
    BasicText(
        text = message,
        modifier = Modifier.testTag("greeting")
    )
}

@Composable
private fun CounterScreen() {
    var count by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.testTag("counter_screen")) {
        BasicText(text = "Count: $count")
        BasicText(
            text = "Increment",
            modifier = Modifier
                .testTag("increment_button")
                .clickable { count++ }
        )
    }
}
