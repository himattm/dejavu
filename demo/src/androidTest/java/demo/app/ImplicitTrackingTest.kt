package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Implicit tracking end-to-end tests.
 *
 * These tests prove that recomposition tracking works with ZERO per-composable
 * tracking modifiers. Composables only use normal Modifier.testTag().
 * DejavuTracer implicitly tracks recompositions via Composer.setTracer() and
 * resolves testTags to composable function names via the Group tree.
 */
@RunWith(AndroidJUnit4::class)
class ImplicitTrackingTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<MainActivity>()

    @Test
    fun counterValue_recomposesOnClick() {
        // Before click: initial composition only
        composeTestRule.onNodeWithTag("counter_value").assertStable()

        // Click increment
        composeTestRule.onNodeWithTag("inc_button").performClick()

        // CounterValue should have recomposed once
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = 1)
    }

    @Test
    fun counterTitle_staysStable() {
        // Click increment multiple times
        repeat(3) {
            composeTestRule.onNodeWithTag("inc_button").performClick()
        }

        // CounterTitle should never recompose (no inputs changed)
        composeTestRule.onNodeWithTag("counter_title").assertStable()
    }

    @Test
    fun multipleClicks_tracksCumulatively() {
        // Click 3 times
        repeat(3) {
            composeTestRule.onNodeWithTag("inc_button").performClick()
        }

        // CounterValue should have recomposed at least 3 times
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = 3)
    }

    @Test
    fun reset_tracksRecomposition() {
        // Click inc then reset
        composeTestRule.onNodeWithTag("inc_button").performClick()
        composeTestRule.onNodeWithTag("reset_button").performClick()

        // CounterValue should have recomposed at least twice
        composeTestRule.onNodeWithTag("counter_value").assertRecompositions(atLeast = 2)
    }
}
