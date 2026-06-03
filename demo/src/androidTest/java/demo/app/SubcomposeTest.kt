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
        composeTestRule.waitForIdle()
        // 1: toggling widthToggle changes the Box's fillMaxWidth fraction, so BoxWithConstraints
        //    feeds new constraints into the subcomposition and ConstraintReader recomposes once
        //    with a new maxWidth param (verified by SubcomposePatternTest: constraint change → 1).
        composeTestRule.onNodeWithTag("constraint_reader").assertRecompositions(exactly = 1)
    }

    @Test
    fun noConstraintChange_subcompositionStable() {
        // trigger_parent_btn increments triggerParent which recomposes the parent
        // but Box width doesn't change, so BoxWithConstraints subcomposition stays stable
        composeTestRule.onNodeWithTag("trigger_parent_btn").performClick()
        composeTestRule.waitForIdle()
        // 0: parent recomposes but constraints are unchanged, so the subcomposed reader is stable
        //    (verified by SubcomposePatternTest: unchanged constraints → 0).
        composeTestRule.onNodeWithTag("constraint_reader").assertStable()
    }

    @Test
    fun animateAsState_producesMultipleRecompositions() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("animate_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        // KEEP directional: animateIntAsState recomposes anim_value_reader once per frame the rounded
        // Int changes. The exact count is an animation/easing/clock-cadence moving target across
        // Compose versions and devices (observed 17 here), so a pinned literal would be brittle. The
        // point of this test is that the animation produces MANY recompositions — assert that floor.
        composeTestRule.onNodeWithTag("anim_value_reader").assertRecompositions(atLeast = 2)
    }

    @Test
    fun animateAsState_settlesAfterAnimation() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("animate_btn").performClick()
        // Let animation complete
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Reset and wait more — animation should be done, no more recomps
        composeTestRule.resetRecompositionCounts()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        // 0: the 300ms tween has fully settled before the reset, so no further frames change the
        //    animated value and the reader is stable after the reset baseline.
        composeTestRule.onNodeWithTag("anim_value_reader").assertStable()
    }

    @Test
    fun moveContent_preservesState_noRecomposition() {
        // Move from slot A to slot B
        composeTestRule.onNodeWithTag("move_content_btn").performClick()
        composeTestRule.waitForIdle()
        // movableContentOf moves state without re-executing — should be stable
        // 0: relocating the movable content from slot A to slot B preserves the same composition
        //    instance, so MovableChild does not recompose (verified by SubcomposePatternTest).
        composeTestRule.onNodeWithTag("movable_child").assertStable()
    }

    @Test
    fun moveContentTwice_backToOriginalSlot_stillStable() {
        // Move A→B→A
        composeTestRule.onNodeWithTag("move_content_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("move_content_btn").performClick()
        composeTestRule.waitForIdle()
        // 0: round-tripping the movable content (A→B→A) preserves the instance throughout, so the
        //    child never recomposes (verified by SubcomposePatternTest).
        composeTestRule.onNodeWithTag("movable_child").assertStable()
    }

    @Test
    fun nonRestartable_recomposesWithParent() {
        composeTestRule.onNodeWithTag("trigger_parent_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: a @NonRestartableComposable re-runs whenever its caller does; one triggerParent bump
        //    recomposes the root once, re-running NonRestartableChild with a new param exactly once
        //    (verified by SubcomposePatternTest).
        composeTestRule.onNodeWithTag("non_restartable").assertRecompositions(exactly = 1)
    }

    @Test
    fun nonRestartable_vs_regular_bothRecomposeOnParamChange() {
        composeTestRule.onNodeWithTag("trigger_parent_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: NonRestartableChild re-runs once with the new parentTrigger param (cannot skip).
        composeTestRule.onNodeWithTag("non_restartable").assertRecompositions(exactly = 1)
        // 1: RegularChild's param (parentTrigger) actually changed, so strong skipping cannot skip
        //    it; it recomposes once with the new value.
        composeTestRule.onNodeWithTag("regular_child").assertRecompositions(exactly = 1)
    }

    @Test
    fun animateAsState_nonRestartableCannotSkip_regularCanSkip() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("animate_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        // KEEP directional: the animated value is read in the root scope, so every animation frame
        // that changes it recomposes the root, and @NonRestartableComposable NonRestartableChild
        // re-runs on each of those frames. The exact frame count is an animation moving target, so
        // assert the floor (it recomposes many times). The meaningful contrast is the exact
        // regular_child count below.
        composeTestRule.onNodeWithTag("non_restartable").assertRecompositions(atLeast = 2)
        // 0: RegularChild's param (parentTrigger) is unchanged during the animation, so strong
        //    skipping skips it on every frame — it stays stable.
        composeTestRule.onNodeWithTag("regular_child").assertStable()
    }
}
