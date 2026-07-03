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
 * Cross-platform port of Android DejavuLibraryCorrectnessTest.
 * Validates core library behaviors: reset semantics, fresh state per test,
 * stable composable tracking, and cumulative interaction counting.
 * Skips 3 lifecycle tests that require Dejavu.enable(application).
 *
 * Every recomposition assertion is exact and self-validating against a [GroundTruth] `SideEffect`
 * (the runtime runs the effect after every successful composition, so it is the real composition
 * count). Both tracked nodes here are single-instance with a unique testTag — `CorrectnessTitle`
 * (`correctness_title`) and `CorrectnessValue` (`correctness_value`) — so the public per-tag API
 * resolves their exact count on all platforms → `exactly = GroundTruth.delta(tag)`.
 *
 * Every test calls [resetRecompositionCounts] + [GroundTruth.snapshotBaseline] at the point Dejavu's
 * counts are zeroed, keeping `delta` aligned with the tracer's post-baseline count.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryCorrectnessPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    @Test
    fun reset_clearsCounts() = runComposeUiTest {
        setContent { DejavuTestContent { CorrectnessCounter() } }
        waitForIdle()

        // Generate some recompositions
        repeat(2) {
            onNodeWithTag("correctness_inc").performClick()
            waitForIdle()
        }

        // Reset should clear counts
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // After reset, stable composable should show 0
        onNodeWithTag("correctness_value").assertStable()
        assertEquals(0, GroundTruth.delta("correctness_value"), "no recomposition recorded immediately after reset")

        // Click again to verify tracking resumes
        onNodeWithTag("correctness_inc").performClick()
        waitForIdle()

        onNodeWithTag("correctness_value")
            .assertRecompositions(exactly = GroundTruth.delta("correctness_value"))
        assertEquals(1, GroundTruth.delta("correctness_value"), "one click after reset recomposes the value once")
    }

    @Test
    fun autoReset_freshStatePerTest() = runComposeUiTest {
        setContent { DejavuTestContent { CorrectnessCounter() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // On first frame with no interactions, counter_value should be stable
        // (setUp calls enableDejavuForTest which resets all state)
        onNodeWithTag("correctness_value").assertStable()
        assertEquals(0, GroundTruth.delta("correctness_value"), "no interaction means no recomposition")
    }

    @Test
    fun stableComposable_neverRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { CorrectnessCounter() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Click 3 times — only counter_value should recompose, not the title
        repeat(3) {
            onNodeWithTag("correctness_inc").performClick()
            waitForIdle()
        }

        // Title has no changing inputs — should remain stable
        onNodeWithTag("correctness_title")
            .assertRecompositions(exactly = GroundTruth.delta("correctness_title"))
        assertEquals(0, GroundTruth.delta("correctness_title"), "parameterless title never recomposes")
    }

    @Test
    fun multipleInteractions_tracksCumulatively() = runComposeUiTest {
        setContent { DejavuTestContent { CorrectnessCounter() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        repeat(3) {
            onNodeWithTag("correctness_inc").performClick()
            waitForIdle()
        }

        // Each click changes `count`, recomposing the value node exactly once → 3 cumulative.
        onNodeWithTag("correctness_value")
            .assertRecompositions(exactly = GroundTruth.delta("correctness_value"))
        assertEquals(3, GroundTruth.delta("correctness_value"), "three clicks recompose the value three times")
    }
}

// ══════════════════════════════════════════════════════════════
// Test Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun CorrectnessTitle() {
    SideEffect { GroundTruth.record("correctness_title") }
    BasicText("Title", Modifier.testTag("correctness_title"))
}

@Composable
private fun CorrectnessValue(count: Int) {
    SideEffect { GroundTruth.record("correctness_value") }
    BasicText("Count: $count", Modifier.testTag("correctness_value"))
}

@Composable
private fun CorrectnessCounter() {
    var count by remember { mutableIntStateOf(0) }
    Column {
        CorrectnessTitle()
        CorrectnessValue(count)
        BasicText("Inc", Modifier.testTag("correctness_inc").clickable { count++ })
    }
}
