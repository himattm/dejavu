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
 * Cross-platform port of Android DejavuLibraryCorrectnessTest.
 * Validates core library behaviors: reset semantics, fresh state per test,
 * stable composable tracking, and cumulative interaction counting.
 * Skips 3 lifecycle tests that require Dejavu.enable(application).
 */
@OptIn(ExperimentalTestApi::class)
class LibraryCorrectnessPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
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

        // After reset, stable composable should show 0
        onNodeWithTag("correctness_value").assertStable()

        // Click again to verify tracking resumes
        onNodeWithTag("correctness_inc").performClick()
        waitForIdle()

        onNodeWithTag("correctness_value").assertRecompositions(exactly = 1)
    }

    @Test
    fun autoReset_freshStatePerTest() = runComposeUiTest {
        setContent { DejavuTestContent { CorrectnessCounter() } }
        waitForIdle()

        // On first frame with no interactions, counter_value should be stable
        // (setUp calls enableDejavuForTest which resets all state)
        onNodeWithTag("correctness_value").assertStable()
    }

    @Test
    fun stableComposable_neverRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { CorrectnessCounter() } }
        waitForIdle()

        // Click 3 times — only counter_value should recompose, not the title
        repeat(3) {
            onNodeWithTag("correctness_inc").performClick()
            waitForIdle()
        }

        // Title has no changing inputs — should remain stable
        onNodeWithTag("correctness_title").assertStable()
    }

    @Test
    fun multipleInteractions_tracksCumulatively() = runComposeUiTest {
        setContent { DejavuTestContent { CorrectnessCounter() } }
        waitForIdle()

        repeat(3) {
            onNodeWithTag("correctness_inc").performClick()
            waitForIdle()
        }

        onNodeWithTag("correctness_value").assertRecompositions(atLeast = 3)
    }
}

// ══════════════════════════════════════════════════════════════
// Test Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun CorrectnessTitle() {
    BasicText("Title", Modifier.testTag("correctness_title"))
}

@Composable
private fun CorrectnessCounter() {
    var count by remember { mutableIntStateOf(0) }
    Column {
        CorrectnessTitle()
        BasicText("Count: $count", Modifier.testTag("correctness_value"))
        BasicText("Inc", Modifier.testTag("correctness_inc").clickable { count++ })
    }
}
