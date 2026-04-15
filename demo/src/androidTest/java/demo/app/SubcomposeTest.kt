package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubcomposeTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<SubcomposeActivity>()

    @Test
    fun constraintChange_recomposesSubcomposition() {
        composeTestRule.onNodeWithTag("toggle_width_btn").performClick()
        composeTestRule.onNodeWithTag("constraint_reader").assertRecompositions(atLeast = 1)
    }

    @Test
    fun noConstraintChange_subcompositionStable() {
        // trigger_parent_btn increments triggerParent which recomposes the parent
        // but Box width doesn't change, so BoxWithConstraints subcomposition stays stable
        composeTestRule.onNodeWithTag("trigger_parent_btn").performClick()
        composeTestRule.onNodeWithTag("constraint_reader").assertStable()
    }

    @Test
    fun animateAsState_producesMultipleRecompositions() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("animate_btn").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithTag("anim_value_reader").assertRecompositions(atLeast = 2)
    }

    @Test
    fun animateAsState_settlesAfterAnimation() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("animate_btn").performClick()
        // Let animation complete
        composeTestRule.mainClock.advanceTimeBy(500)

        // Reset and wait more — animation should be done, no more recomps
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithTag("anim_value_reader").assertStable()
    }

    @Test
    fun moveContent_preservesState_noRecomposition() {
        // Move from slot A to slot B
        composeTestRule.onNodeWithTag("move_content_btn").performClick()
        // movableContentOf moves state without re-executing — should be stable
        // NOTE: This is a discovery test. If Dejavu sees a traceEventStart during
        // the move, this will fail, revealing the edge case.
        composeTestRule.onNodeWithTag("movable_child").assertStable()
    }

    @Test
    fun moveContentTwice_backToOriginalSlot_stillStable() {
        // Move A→B→A
        composeTestRule.onNodeWithTag("move_content_btn").performClick()
        composeTestRule.onNodeWithTag("move_content_btn").performClick()
        composeTestRule.onNodeWithTag("movable_child").assertStable()
    }

    @Test
    fun nonRestartable_recomposesWithParent() {
        composeTestRule.onNodeWithTag("trigger_parent_btn").performClick()
        composeTestRule.onNodeWithTag("non_restartable").assertRecompositions(exactly = 1)
    }

    @Test
    fun nonRestartable_vs_regular_bothRecomposeOnParamChange() {
        composeTestRule.onNodeWithTag("trigger_parent_btn").performClick()
        composeTestRule.onNodeWithTag("non_restartable").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("regular_child").assertRecompositions(exactly = 1)
    }

    @Test
    fun animateAsState_nonRestartableCannotSkip_regularCanSkip() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("animate_btn").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        // @NonRestartableComposable can't skip — recomposes with parent every frame
        composeTestRule.onNodeWithTag("non_restartable").assertRecompositions(atLeast = 2)
        // Regular composable with unchanged params skips via strong skipping
        composeTestRule.onNodeWithTag("regular_child").assertStable()
    }
    @org.junit.After
    fun tearDown() {
        composeTestRule.mainClock.autoAdvance = true
    }
}
