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
        // 1: changing the data set hands DonutChart a new `data` list, recomposing it once.
        composeTestRule.onNodeWithTag("donut_chart").assertRecompositions(exactly = 1)
    }

    @Test
    fun chart_legend_recomposes_on_data_change() {
        composeTestRule.onNodeWithTag("change_data_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: changing the data set hands ChartLegend a new `data` list, recomposing it once.
        composeTestRule.onNodeWithTag("chart_legend").assertRecompositions(exactly = 1)
    }

    @Test
    fun chart_selected_legend_item_recomposes() {
        composeTestRule.onNodeWithTag("select_segment_0_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: selecting segment 0 flips legend_item_0's isSelected false->true, recomposing it once.
        composeTestRule.onNodeWithTag("legend_item_0").assertRecompositions(exactly = 1)
    }

    @Test
    fun chart_unselected_legend_items_stable() {
        composeTestRule.onNodeWithTag("select_segment_0_btn").performClick()
        composeTestRule.waitForIdle()
        // Only legend_item_0 changes (isSelected false->true); items 1-4 keep every param
        // (index/label/color/percentage/isSelected all unchanged) so Compose skips them. With
        // Android per-instance tracking each unselected item resolves to exactly 0 -> stable.
        composeTestRule.onNodeWithTag("legend_item_1").assertStable()
        composeTestRule.onNodeWithTag("legend_item_2").assertStable()
        composeTestRule.onNodeWithTag("legend_item_3").assertStable()
        composeTestRule.onNodeWithTag("legend_item_4").assertStable()
    }

    @Test
    fun chart_clear_selection_recomposes_selected() {
        composeTestRule.onNodeWithTag("select_segment_0_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("clear_selection_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: clearing selection flips legend_item_0's isSelected true->false, recomposing it once.
        composeTestRule.onNodeWithTag("legend_item_0").assertRecompositions(exactly = 1)
    }

    @Test
    fun chart_change_data_button_stable() {
        composeTestRule.onNodeWithTag("change_data_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("change_data_btn").assertStable()
    }

    @Test
    fun chart_no_recomposition_without_interaction() {
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
