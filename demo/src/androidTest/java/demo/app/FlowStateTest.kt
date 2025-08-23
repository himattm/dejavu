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
class FlowStateTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<FlowStateActivity>()

    @Test
    fun flowEmission_recomposesCollector() {
        composeTestRule.onNodeWithTag("flow_inc_btn").performClick()
        composeTestRule.onNodeWithTag("flow_counter").assertRecompositions(exactly = 1)
    }

    @Test
    fun multipleFlowEmissions_eachTracked() {
        repeat(3) {
            composeTestRule.onNodeWithTag("flow_inc_btn").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("flow_counter").assertRecompositions(exactly = 3)
    }

    @Test
    fun flowEmission_doesNotAffectUnrelatedComposables() {
        composeTestRule.onNodeWithTag("flow_inc_btn").performClick()
        composeTestRule.onNodeWithTag("same_value_reader").assertStable()
        composeTestRule.onNodeWithTag("list_size_reader").assertStable()
    }

    @Test
    fun threeFlowEmissionsInOneFrame_coalesceToOneRecomposition() {
        composeTestRule.onNodeWithTag("flow_inc_three_btn").performClick()
        composeTestRule.onNodeWithTag("flow_counter").assertRecompositions(exactly = 1)
    }

    @Test
    fun multipleStateWritesInOneCallback_coalescesToOneRecomposition() {
        composeTestRule.onNodeWithTag("coalesce_btn").performClick()
        composeTestRule.onNodeWithTag("batch_reader_a").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("batch_reader_b").assertRecompositions(exactly = 1)
    }

    @Test
    fun sameValueWrite_doesNotCauseRecomposition() {
        composeTestRule.onNodeWithTag("same_value_write_btn").performClick()
        composeTestRule.onNodeWithTag("same_value_reader").assertStable()
    }

    @Test
    fun realValueWrite_doesCauseRecomposition() {
        composeTestRule.onNodeWithTag("real_value_write_btn").performClick()
        composeTestRule.onNodeWithTag("same_value_reader").assertRecompositions(exactly = 1)
    }

    @Test
    fun addToList_recomposesListReader() {
        composeTestRule.onNodeWithTag("add_item_btn").performClick()
        composeTestRule.onNodeWithTag("list_size_reader").assertRecompositions(exactly = 1)
    }

    @Test
    fun multipleAdds_eachTracked() {
        repeat(3) {
            composeTestRule.onNodeWithTag("add_item_btn").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("list_size_reader").assertRecompositions(exactly = 3)
    }

    @Test
    fun batchSnapshotWrite_producesOneRecomposition() {
        composeTestRule.onNodeWithTag("batch_btn").performClick()
        composeTestRule.onNodeWithTag("batch_reader_a").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("batch_reader_b").assertRecompositions(exactly = 1)
    }
}
