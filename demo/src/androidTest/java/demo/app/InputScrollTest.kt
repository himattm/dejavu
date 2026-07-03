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

@RunWith(AndroidJUnit4::class)
class InputScrollTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<InputScrollActivity>()

    @Test
    fun typeCharacter_displayRecomposesOnce() {
        composeTestRule.onNodeWithTag("type_char_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: one keystroke flips `text`, the value-reader displays it → recomposes exactly once
        // (verified in InputScrollPatternTest: delta("text_display") == 1).
        composeTestRule.onNodeWithTag("text_display").assertRecompositions(exactly = 1)
    }

    @Test
    fun typeCharacter_unrelatedStable() {
        composeTestRule.onNodeWithTag("type_char_btn").performClick()
        composeTestRule.waitForIdle()
        // parameterless sibling never recomposes on typing
        // (verified in InputScrollPatternTest: delta("text_unrelated") == 0).
        composeTestRule.onNodeWithTag("text_unrelated").assertStable()
    }

    @Test
    fun rapidTyping_eachKeystrokeTracked() {
        repeat(5) {
            composeTestRule.onNodeWithTag("type_char_btn").performClick()
            composeTestRule.waitForIdle()
        }
        // 5: five distinct settled keystrokes, each flips `text` once → five recompositions
        // (verified in InputScrollPatternTest: delta("text_display") == 5).
        composeTestRule.onNodeWithTag("text_display").assertRecompositions(exactly = 5)
    }

    @Test
    fun scrollDown_positionReaderRecomposes() {
        composeTestRule.onNodeWithTag("scroll_down_btn").performClick()
        composeTestRule.waitForIdle()
        // Reading firstVisibleItemIndex in composition causes recomposition on scroll.
        // KEEP: scroll frame count is a moving target — a programmatic scroll settles over a
        // frame-dependent number of position updates, so this stays directional, not pinned.
        composeTestRule.onNodeWithTag("scroll_position").assertRecompositions(atLeast = 1)
    }

    @Test
    fun scrollDown_textDisplayStable() {
        composeTestRule.onNodeWithTag("scroll_down_btn").performClick()
        composeTestRule.waitForIdle()
        // scrolling the list does not touch `text`, so the value-reader stays stable.
        composeTestRule.onNodeWithTag("text_display").assertStable()
    }

    @Test
    fun produceState_recomposesOnTriggerChange() {
        composeTestRule.onNodeWithTag("change_source_btn").performClick()
        composeTestRule.waitForIdle()
        // produceState restarts when trigger changes, emitting one new value.
        // 1: one trigger change relaunches the producer, which writes a single new `produced`
        //    value → one recomposition once settled.
        // FLAG (uncertain): produceState is asynchronous; the common test (InputScrollPatternTest)
        //    deliberately pins delta(tag) rather than a literal and only asserts delta >= 1, because
        //    the new value can land across more than one frame. Verify on emulator; if it settles
        //    to >1, keep this directional (atLeast = 1) instead.
        composeTestRule.onNodeWithTag("produced_value").assertRecompositions(exactly = 1)
    }

    @Test
    fun snapshotFlowChange_recomposesReader() {
        composeTestRule.onNodeWithTag("change_source_btn").performClick()
        composeTestRule.waitForIdle()
        // snapshotFlow delivers the new value to the collector, which writes `flowValue` once.
        // 1: one source change → one new emission → reader recomposes once after it settles.
        // FLAG (uncertain): snapshotFlow emission is asynchronous; the common test
        //    (InputScrollPatternTest) pins delta(tag) (not a literal) and only asserts delta >= 1,
        //    because the emission can deliver across more than one frame. Verify on emulator; if it
        //    settles to >1, keep this directional (atLeast = 1) instead.
        composeTestRule.onNodeWithTag("flow_reader").assertRecompositions(exactly = 1)
    }

    @Test
    fun snapshotFlowSameValue_readerStable() {
        // Same-value write should not trigger snapshotFlow emission.
        composeTestRule.onNodeWithTag("same_source_btn").performClick()
        composeTestRule.waitForIdle()
        // same-value source write emits nothing new → reader stable
        // (verified in InputScrollPatternTest: delta("flow_reader") == 0).
        composeTestRule.onNodeWithTag("flow_reader").assertStable()
    }
}
