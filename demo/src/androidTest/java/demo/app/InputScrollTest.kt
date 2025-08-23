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
        composeTestRule.onNodeWithTag("text_display").assertRecompositions(exactly = 1)
    }

    @Test
    fun typeCharacter_unrelatedStable() {
        composeTestRule.onNodeWithTag("type_char_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("text_unrelated").assertStable()
    }

    @Test
    fun rapidTyping_eachKeystrokeTracked() {
        repeat(5) {
            composeTestRule.onNodeWithTag("type_char_btn").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("text_display").assertRecompositions(exactly = 5)
    }

    @Test
    fun scrollDown_positionReaderRecomposes() {
        composeTestRule.onNodeWithTag("scroll_down_btn").performClick()
        composeTestRule.waitForIdle()
        // Reading firstVisibleItemIndex in composition causes recomposition on scroll
        composeTestRule.onNodeWithTag("scroll_position").assertRecompositions(atLeast = 1)
    }

    @Test
    fun scrollDown_textDisplayStable() {
        composeTestRule.onNodeWithTag("scroll_down_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("text_display").assertStable()
    }

    @Test
    fun produceState_recomposesOnTriggerChange() {
        composeTestRule.onNodeWithTag("change_source_btn").performClick()
        composeTestRule.waitForIdle()
        // produceState restarts when trigger changes, emitting a new value
        composeTestRule.onNodeWithTag("produced_value").assertRecompositions(atLeast = 1)
    }

    @Test
    fun snapshotFlowChange_recomposesReader() {
        composeTestRule.onNodeWithTag("change_source_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("flow_reader").assertRecompositions(atLeast = 1)
    }

    @Test
    fun snapshotFlowSameValue_readerStable() {
        // Same-value write should not trigger snapshotFlow emission
        composeTestRule.onNodeWithTag("same_source_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("flow_reader").assertStable()
    }
}
