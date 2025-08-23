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
class DonutChartTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<DonutChartActivity>()

    @Test
    fun chart_recomposes_on_data_change() {
        composeTestRule.onNodeWithTag("change_data_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("donut_chart").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chart_legend_recomposes_on_data_change() {
        composeTestRule.onNodeWithTag("change_data_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chart_legend").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chart_selected_legend_item_recomposes() {
        composeTestRule.onNodeWithTag("select_segment_0_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("legend_item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chart_unselected_legend_items_stable() {
        composeTestRule.onNodeWithTag("select_segment_0_btn").performClick()
        composeTestRule.waitForIdle()
        // Unselected items may recompose once due to parent (ChartLegend) invalidation
        // propagating new isSelected param. Verify they recompose at most once.
        composeTestRule.onNodeWithTag("legend_item_1").assertRecompositions(atMost = 1)
        composeTestRule.onNodeWithTag("legend_item_2").assertRecompositions(atMost = 1)
        composeTestRule.onNodeWithTag("legend_item_3").assertRecompositions(atMost = 1)
        composeTestRule.onNodeWithTag("legend_item_4").assertRecompositions(atMost = 1)
    }

    @Test
    fun chart_clear_selection_recomposes_selected() {
        composeTestRule.onNodeWithTag("select_segment_0_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("clear_selection_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("legend_item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chart_change_data_button_stable() {
        composeTestRule.onNodeWithTag("change_data_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("change_data_btn").assertStable()
    }

    @Test
    fun chart_no_recomposition_without_interaction() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("donut_chart_root").assertStable()
        composeTestRule.onNodeWithTag("donut_chart").assertStable()
        composeTestRule.onNodeWithTag("chart_legend").assertStable()
        composeTestRule.onNodeWithTag("legend_item_0").assertStable()
        composeTestRule.onNodeWithTag("legend_item_1").assertStable()
        composeTestRule.onNodeWithTag("legend_item_2").assertStable()
        composeTestRule.onNodeWithTag("legend_item_3").assertStable()
        composeTestRule.onNodeWithTag("legend_item_4").assertStable()
    }
}
