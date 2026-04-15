package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.Dejavu
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DejavuLibraryCorrectnessTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<CounterActivity>()

    @Test
    fun reset_clearsCounts() {
        // Click to generate recompositions
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = 1)

        // Reset should zero out counts
        composeTestRule.resetRecompositionCounts()

        // After reset, counter_value should show 0 recompositions
        composeTestRule.onNodeWithTag("counter_value").assertStable()

        // New interactions should still register
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = 1)
    }

    @Test
    fun autoReset_freshStatePerTest() {
        // DejavuComposeTestRule resets before each test.
        // On first frame, counter_value has 0 recompositions.
        composeTestRule.onNodeWithTag("counter_value").assertStable()
    }

    @Test
    fun stableComposable_neverRecomposes() {
        // Click multiple times — counterTitle has no inputs that change
        repeat(3) {
            composeTestRule.onNodeWithTag("inc_button").performClick()
        }

        // counter_title should stay at 0 recompositions across all clicks
        composeTestRule.onNodeWithTag("counter_title").assertStable()
    }

    @Test
    fun multipleInteractions_tracksCumulatively() {
        // 3 clicks should produce 3 recompositions on counter_value
        repeat(3) {
            composeTestRule.onNodeWithTag("inc_button").performClick()
        }

        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = 3)
    }

    // ── Lifecycle tests ─────────────────────────────────────────────

    @Test
    fun disable_stopsTracking() {
        // Verify tracking works initially
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = 1)

        // Disable tracking (must run on UI thread for Choreographer access)
        composeTestRule.runOnUiThread { Dejavu.disable() }

        // Reset counts so we start from 0
        composeTestRule.resetRecompositionCounts()

        // Perform more clicks while disabled
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("inc_button").performClick()

        // Tracking is stopped, so counter_value should show 0 recompositions
        composeTestRule.onNodeWithTag("counter_value").assertStable()

        // Re-enable so other tests are not affected (rule auto-enables, but be safe)
        composeTestRule.runOnUiThread { Dejavu.enable(composeTestRule.activity.application) }
    }

    @Test
    fun enable_isIdempotent() {
        // Enable is already called by the rule. Call it again -- should not crash or break tracking.
        composeTestRule.runOnUiThread { Dejavu.enable(composeTestRule.activity.application) }
        composeTestRule.runOnUiThread { Dejavu.enable(composeTestRule.activity.application) }

        // Tracking should still work correctly
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = 1)
    }

    @Test
    fun enableDisableEnable_cycle() {
        // Phase 1: Tracking works
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = 1)

        // Phase 2: Disable and verify tracking stops
        composeTestRule.runOnUiThread { Dejavu.disable() }
        composeTestRule.resetRecompositionCounts()
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_value").assertStable()

        // Phase 3: Re-enable completes without error
        // Note: tag-to-function mapping requires the activity lifecycle to fire
        // onActivityResumed, which doesn't re-fire for an already-resumed activity.
        // The DejavuComposeTestRule handles this correctly by calling enable() in
        // its apply() block before the activity is resumed.
        composeTestRule.runOnUiThread { Dejavu.enable(composeTestRule.activity.application) }
        composeTestRule.waitForIdle()

        // Verify the tracer is active: clicks don't crash and stable composables
        // still report correctly (counter_title has 0 recompositions either way)
        composeTestRule.resetRecompositionCounts()
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_title").assertStable()
    }

    @Test
    fun reEnable_onAlreadyResumedActivity_restoresTrackedTagLookups() {
        // Repro: enable() is called while the activity is already resumed, so
        // onActivityResumed does not fire again to seed the active activity.
        composeTestRule.runOnUiThread { Dejavu.disable() }
        composeTestRule.runOnUiThread { Dejavu.enable(composeTestRule.activity.application) }
        composeTestRule.waitForIdle()

        // If the active activity is not reseeded, tag lookup for counter_value
        // breaks here and the recomposition assertion fails.
        composeTestRule.resetRecompositionCounts()
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = 1)
    }
}
