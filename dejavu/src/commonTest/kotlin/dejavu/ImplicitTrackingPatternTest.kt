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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Cross-platform port of Android ImplicitTrackingTest.
 * Simple counter with title, value, increment, and reset buttons.
 * Validates implicit recomposition tracking with zero per-composable modifiers.
 */
@OptIn(ExperimentalTestApi::class)
class ImplicitTrackingPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun counterValue_recomposesOnClick() = runComposeUiTest {
        setContent { DejavuTestContent { CounterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("inc_button").performClick()
        waitForIdle()

        onNodeWithTag("counter_value").assertRecompositions(exactly = 1)
    }

    @Test
    fun counterTitle_staysStable() = runComposeUiTest {
        setContent { DejavuTestContent { CounterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        repeat(3) {
            onNodeWithTag("inc_button").performClick()
            waitForIdle()
        }

        onNodeWithTag("counter_title").assertStable()
    }

    @Test
    fun multipleClicks_tracksCumulatively() = runComposeUiTest {
        setContent { DejavuTestContent { CounterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        repeat(3) {
            onNodeWithTag("inc_button").performClick()
            waitForIdle()
        }

        onNodeWithTag("counter_value").assertRecompositions(atLeast = 3)
    }

    @Test
    fun reset_tracksRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { CounterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("inc_button").performClick()
        waitForIdle()
        onNodeWithTag("reset_button").performClick()
        waitForIdle()

        onNodeWithTag("counter_value").assertRecompositions(atLeast = 2)
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun CounterScreen() {
    var count by remember { mutableIntStateOf(0) }
    Column {
        CounterTitle()
        CounterValue(count)
        IncButton { count++ }
        ResetButton { count = 0 }
    }
}

@Composable
private fun CounterTitle() {
    BasicText("Dejavu Counter", Modifier.testTag("counter_title"))
}

@Composable
private fun CounterValue(value: Int) {
    BasicText("Value: $value", Modifier.testTag("counter_value"))
}

@Composable
private fun IncButton(onClick: () -> Unit) {
    BasicText("Inc", Modifier.testTag("inc_button").clickable(onClick = onClick))
}

@Composable
private fun ResetButton(onClick: () -> Unit) {
    BasicText("Reset", Modifier.testTag("reset_button").clickable(onClick = onClick))
}
