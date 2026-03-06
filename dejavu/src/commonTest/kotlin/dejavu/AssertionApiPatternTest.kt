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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Cross-platform port of Android AssertionApiTest.
 * Validates assertion API parameter validation, error messages,
 * range modes, and chaining behavior.
 */
@OptIn(ExperimentalTestApi::class)
class AssertionApiPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    // ── Parameter validation ─────────────────────────────────────

    @Test
    fun negativeExactly_throws() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()
        val threw = try {
            onNodeWithTag("counter_title").assertRecompositions(exactly = -1)
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(threw, "Expected IllegalArgumentException for exactly = -1")
    }

    @Test
    fun negativeAtLeast_throws() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()
        val threw = try {
            onNodeWithTag("counter_title").assertRecompositions(atLeast = -1)
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(threw, "Expected IllegalArgumentException for atLeast = -1")
    }

    @Test
    fun negativeAtMost_throws() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()
        val threw = try {
            onNodeWithTag("counter_title").assertRecompositions(atMost = -1)
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(threw, "Expected IllegalArgumentException for atMost = -1")
    }

    @Test
    fun exactlyWithAtLeast_throws() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()
        val threw = try {
            onNodeWithTag("counter_title").assertRecompositions(exactly = 1, atLeast = 1)
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(threw, "Expected IllegalArgumentException for exactly + atLeast")
    }

    @Test
    fun atLeastGreaterThanAtMost_throws() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()
        val threw = try {
            onNodeWithTag("counter_title").assertRecompositions(atLeast = 5, atMost = 2)
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(threw, "Expected IllegalArgumentException for atLeast > atMost")
    }

    // ── Missing testTag ──────────────────────────────────────────

    @Test
    fun nodeWithoutTestTag_failsWithHelpfulMessage() = runComposeUiTest {
        setContent {
            DejavuTestContent {
                BasicText("some text")
            }
        }
        waitForIdle()

        val error = try {
            onNodeWithText("some text", useUnmergedTree = true).assertStable()
            null
        } catch (e: AssertionError) {
            e
        }
        assertTrue(error != null, "Expected an AssertionError for missing testTag")
        assertTrue(
            error.message?.contains("testTag") == true,
            "Error message should mention 'testTag', got: ${error.message}"
        )
    }

    // ── Range mode ───────────────────────────────────────────────

    @Test
    fun rangeMode_withinRange_passes() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()

        repeat(2) {
            onNodeWithTag("inc_button").performClick()
            waitForIdle()
        }

        onNodeWithTag("counter_value").assertRecompositions(atLeast = 1, atMost = 5)
    }

    @Test
    fun rangeMode_belowRange_fails() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()

        // No clicks — counter_value should have 0 recompositions
        val error = try {
            onNodeWithTag("counter_value").assertRecompositions(atLeast = 5, atMost = 10)
            null
        } catch (e: AssertionError) {
            e
        }
        assertTrue(error != null, "Expected an AssertionError for below-range count")
        val msg = error.message ?: ""
        assertTrue(
            msg.contains("between") || msg.contains("at least"),
            "Error message should contain 'between' or 'at least', got: $msg"
        )
    }

    // ── Descriptive error messages ───────────────────────────────

    @Test
    fun atLeastError_containsDescriptiveMessage() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()

        val error = try {
            onNodeWithTag("counter_value").assertRecompositions(atLeast = 100)
            null
        } catch (e: AssertionError) {
            e
        }
        assertTrue(error != null, "Expected an AssertionError for atLeast=100")
        assertTrue(
            error.message?.contains("at least") == true,
            "Error message should contain 'at least', got: ${error.message}"
        )
    }

    @Test
    fun atMostError_containsDescriptiveMessage() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()

        repeat(3) {
            onNodeWithTag("inc_button").performClick()
            waitForIdle()
        }

        val error = try {
            onNodeWithTag("counter_value").assertRecompositions(atMost = 0)
            null
        } catch (e: AssertionError) {
            e
        }
        assertTrue(error != null, "Expected an AssertionError for atMost=0")
        assertTrue(
            error.message?.contains("at most") == true,
            "Error message should contain 'at most', got: ${error.message}"
        )
    }

    // ── Chaining ─────────────────────────────────────────────────

    @Test
    fun assertStable_returnsSemanticsNodeInteraction_forChaining() = runComposeUiTest {
        setContent { DejavuTestContent { TestCounter() } }
        waitForIdle()

        // assertStable() should return SemanticsNodeInteraction, enabling chaining
        onNodeWithTag("counter_title").assertStable().assertExists()
    }
}

// ══════════════════════════════════════════════════════════════
// Test Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun TestCounter() {
    var count by remember { mutableIntStateOf(0) }
    Column {
        BasicText("Title", Modifier.testTag("counter_title"))
        BasicText("Count: $count", Modifier.testTag("counter_value"))
        BasicText("Inc", Modifier.testTag("inc_button").clickable { count++ })
    }
}
