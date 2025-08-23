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
class ScaffoldSlotsTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ScaffoldSlotsActivity>()

    @Test
    fun changeTopBar_topBarRecomposes() {
        composeTestRule.onNodeWithTag("change_top_btn").performClick()
        composeTestRule.onNodeWithTag("scaffold_top_bar").assertRecompositions(exactly = 1)
    }

    @Test
    fun changeTopBar_bottomBarAndContentStable() {
        composeTestRule.onNodeWithTag("change_top_btn").performClick()
        composeTestRule.onNodeWithTag("scaffold_bottom_bar").assertStable()
        composeTestRule.onNodeWithTag("content_body").assertStable()
    }

    @Test
    fun changeBottom_bottomBarRecomposes() {
        composeTestRule.onNodeWithTag("change_bottom_btn").performClick()
        composeTestRule.onNodeWithTag("scaffold_bottom_bar").assertRecompositions(exactly = 1)
    }

    @Test
    fun changeBottom_topBarStable() {
        composeTestRule.onNodeWithTag("change_bottom_btn").performClick()
        composeTestRule.onNodeWithTag("scaffold_top_bar").assertStable()
    }

    @Test
    fun changeContent_contentBodyRecomposes_staticStable() {
        composeTestRule.onNodeWithTag("change_content_btn").performClick()
        composeTestRule.onNodeWithTag("content_body").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("content_static").assertStable()
    }

    @Test
    fun clickFab_fabClickLabelRecomposes() {
        composeTestRule.onNodeWithTag("scaffold_fab").performClick()
        composeTestRule.onNodeWithTag("fab_click_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun clickFab_topAndBottomBarStable() {
        composeTestRule.onNodeWithTag("scaffold_fab").performClick()
        composeTestRule.onNodeWithTag("scaffold_top_bar").assertStable()
        composeTestRule.onNodeWithTag("scaffold_bottom_bar").assertStable()
    }
}
