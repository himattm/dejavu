package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssertionApiTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<CounterActivity>()

    // ── #10: Negative values rejected ───────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun negativeExactly_throws() {
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeAtLeast_throws() {
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeAtMost_throws() {
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atMost = -1)
    }

    // ── #10: Existing validation still works ────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun exactlyWithAtLeast_throws() {
        composeTestRule.onNodeWithTag("counter_value")
            .assertRecompositions(exactly = 1, atLeast = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun atLeastGreaterThanAtMost_throws() {
        composeTestRule.onNodeWithTag("counter_value")
            .assertRecompositions(atLeast = 5, atMost = 2)
    }

    // ── #4: Untracked composable (no testTag) ───────────────────────

    @Test
    fun nodeWithoutTestTag_failsWithHelpfulMessage() {
        // The inner Text("Inc") inside the Button has no testTag in the unmerged tree
        try {
            composeTestRule.onNodeWithText("Inc", useUnmergedTree = true).assertStable()
            fail("Should have thrown")
        } catch (e: AssertionError) {
            assertTrue(
                "Error should mention testTag, was: ${e.message}",
                e.message?.contains("testTag") == true
            )
        }
    }

    // ── #12: Range mode coverage ────────────────────────────────────

    @Test
    fun rangeMode_withinRange_passes() {
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_value")
            .assertRecompositions(atLeast = 1, atMost = 5)
    }

    @Test
    fun rangeMode_belowRange_fails() {
        try {
            composeTestRule.onNodeWithTag("counter_value")
                .assertRecompositions(atLeast = 5, atMost = 10)
            fail("Should have thrown")
        } catch (e: AssertionError) {
            assertTrue(
                "Error should mention range, was: ${e.message}",
                e.message?.contains("between") == true || e.message?.contains("at least") == true
            )
        }
    }

    // ── #14: Error messages for non-exactly modes ───────────────────

    @Test
    fun atLeastError_containsDescriptiveMessage() {
        try {
            composeTestRule.onNodeWithTag("counter_value")
                .assertRecompositions(atLeast = 100)
            fail("Should have thrown")
        } catch (e: AssertionError) {
            assertTrue(
                "Error should mention 'at least', was: ${e.message}",
                e.message?.contains("at least") == true
            )
        }
    }

    @Test
    fun atMostError_containsDescriptiveMessage() {
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("inc_button").performClick()
        try {
            composeTestRule.onNodeWithTag("counter_value")
                .assertRecompositions(atMost = 0)
            fail("Should have thrown")
        } catch (e: AssertionError) {
            assertTrue(
                "Error should mention 'at most', was: ${e.message}",
                e.message?.contains("at most") == true
            )
        }
    }

    // ── Assertion chaining & equivalence ────────────────────────────

    @Test
    fun assertStable_returnsSemanticsNodeInteraction_forChaining() {
        // assertStable() should return SemanticsNodeInteraction, enabling chaining
        composeTestRule.onNodeWithTag("counter_title")
            .assertStable()
            .assertExists()
    }

    @Test
    fun exactlyZero_behavesLikeAssertStable() {
        // assertRecompositions(exactly = 0) should behave identically to assertStable()
        // Both should pass for a composable that has not recomposed
        composeTestRule.onNodeWithTag("counter_title").assertStable()
        composeTestRule.onNodeWithTag("counter_title").assertRecompositions(exactly = 0)

        // Trigger recompositions on counter_value, then verify both fail the same way
        composeTestRule.onNodeWithTag("inc_button").performClick()

        var stableError: AssertionError? = null
        var exactlyZeroError: AssertionError? = null

        try {
            composeTestRule.onNodeWithTag("counter_value").assertStable()
        } catch (e: AssertionError) {
            stableError = e
        }

        try {
            composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = 0)
        } catch (e: AssertionError) {
            exactlyZeroError = e
        }

        // Both should have thrown
        assertTrue("assertStable should have thrown", stableError != null)
        assertTrue("assertRecompositions(exactly=0) should have thrown", exactlyZeroError != null)

        // Both errors should mention "exactly 0"
        assertTrue(
            "Both should reference exactly 0, stable was: ${stableError?.message}",
            stableError?.message?.contains("exactly 0") == true
        )
        assertTrue(
            "Both should reference exactly 0, exactlyZero was: ${exactlyZeroError?.message}",
            exactlyZeroError?.message?.contains("exactly 0") == true
        )
    }
}
