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
 * Validates that user composables wrapping Material3 framework composables
 * (OutlinedTextField, TextField, Switch, Card) are properly tracked.
 *
 * Regression tests for PR #16: OutlinedTextField tag mapping failure.
 */
@RunWith(AndroidJUnit4::class)
class FrameworkComposableTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<FrameworkComposableActivity>()

    @Test
    fun outlinedTextField_typeCharacter_recomposesWrapper() {
        composeTestRule.onNodeWithTag("type_char_trigger").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_description_input")
            .assertRecompositions(atLeast = 1)
    }

    @Test
    fun textField_typeCharacter_recomposesWrapper() {
        composeTestRule.onNodeWithTag("type_char_trigger").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("simple_text_input")
            .assertRecompositions(atLeast = 1)
    }

    @Test
    fun switch_toggle_recomposesWrapper() {
        composeTestRule.onNodeWithTag("toggle_trigger").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toggle_switch")
            .assertRecompositions(atLeast = 1)
    }

    @Test
    fun card_counterIncrement_recomposesWrapper() {
        composeTestRule.onNodeWithTag("increment_trigger").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("info_card")
            .assertRecompositions(atLeast = 1)
    }

    @Test
    fun unrelatedComposable_typeCharacter_remainsStable() {
        composeTestRule.onNodeWithTag("type_char_trigger").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("counter_label").assertStable()
    }

    @Test
    fun multipleInteractions_allTrackAccurately() {
        composeTestRule.onNodeWithTag("type_char_trigger").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toggle_trigger").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("increment_trigger").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("task_description_input")
            .assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("simple_text_input")
            .assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("toggle_switch")
            .assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("info_card")
            .assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("counter_label")
            .assertRecompositions(atLeast = 1)
    }
}
