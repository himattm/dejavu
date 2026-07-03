package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * Cross-platform port of Android ImplicitTrackingTest.
 * Simple counter with title, value, increment, and reset buttons.
 * Validates implicit recomposition tracking with zero per-composable modifiers.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect`: the Compose
 * runtime runs the effect after every successful composition, so its [GroundTruth.delta] over the
 * post-reset baseline is the node's real recomposition count. Each composable is single-instance
 * with a unique `testTag`, so the public per-tag API resolves the exact count on every platform →
 * `onNodeWithTag(tag).assertRecompositions(exactly = GroundTruth.delta(tag))`. The counts here are
 * also fully deterministic (one state cell, no loops/keys), so each test pins the literal value too.
 *
 * Every test calls [resetRecompositionCounts] + [GroundTruth.snapshotBaseline] after the initial
 * `waitForIdle()` so the tracer's zero point and the ground-truth baseline line up.
 */
@OptIn(ExperimentalTestApi::class)
class ImplicitTrackingPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun counterValue_recomposesOnClick() = runComposeUiTest {
        setContent { DejavuTestContent { CounterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("inc_button").performClick()
        waitForIdle()

        // One increment (count 0→1) recomposes the value once: tracer == ground truth == 1.
        onNodeWithTag("counter_value")
            .assertRecompositions(exactly = GroundTruth.delta("counter_value"))
        assertEquals(1, GroundTruth.delta("counter_value"), "value recomposes once per increment")
    }

    @Test
    fun counterTitle_staysStable() = runComposeUiTest {
        setContent { DejavuTestContent { CounterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        repeat(3) {
            onNodeWithTag("inc_button").performClick()
            waitForIdle()
        }

        // The parameterless title never reads the count, so no increment recomposes it.
        onNodeWithTag("counter_title").assertStable()
        assertEquals(0, GroundTruth.delta("counter_title"), "parameterless title never recomposes")
    }

    @Test
    fun multipleClicks_tracksCumulatively() = runComposeUiTest {
        setContent { DejavuTestContent { CounterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        repeat(3) {
            onNodeWithTag("inc_button").performClick()
            waitForIdle()
        }

        // Three increments each change the count once → exactly three value recompositions.
        onNodeWithTag("counter_value")
            .assertRecompositions(exactly = GroundTruth.delta("counter_value"))
        assertEquals(3, GroundTruth.delta("counter_value"), "three increments recompose the value three times")
    }

    @Test
    fun reset_tracksRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { CounterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("inc_button").performClick()
        waitForIdle()
        onNodeWithTag("reset_button").performClick()
        waitForIdle()

        // Increment (0→1) recomposes the value once; reset (1→0) is a real value change → once more.
        onNodeWithTag("counter_value")
            .assertRecompositions(exactly = GroundTruth.delta("counter_value"))
        assertEquals(2, GroundTruth.delta("counter_value"), "increment then reset recompose the value twice")
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
    SideEffect { GroundTruth.record("counter_title") }
    BasicText("Dejavu Counter", Modifier.testTag("counter_title"))
}

@Composable
private fun CounterValue(value: Int) {
    SideEffect { GroundTruth.record("counter_value") }
    BasicText("Value: $value", Modifier.testTag("counter_value"))
}

@Composable
private fun IncButton(onClick: () -> Unit) {
    SideEffect { GroundTruth.record("inc_button") }
    BasicText("Inc", Modifier.testTag("inc_button").clickable(onClick = onClick))
}

@Composable
private fun ResetButton(onClick: () -> Unit) {
    SideEffect { GroundTruth.record("reset_button") }
    BasicText("Reset", Modifier.testTag("reset_button").clickable(onClick = onClick))
}
