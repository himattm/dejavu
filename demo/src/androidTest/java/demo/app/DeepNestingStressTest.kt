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
class DeepNestingStressTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<DeepNestingStressActivity>()

    @Test
    fun increment_allLevelsRecompose() {
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.onNodeWithTag("level_1").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("level_2").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("level_3").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("level_4").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("level_5").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("level_6").assertRecompositions(exactly = 1)
    }

    @Test
    fun increment_siblingBranchStaysStable() {
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.onNodeWithTag("sibling_branch").assertStable()
        composeTestRule.onNodeWithTag("sibling_child").assertStable()
    }

    @Test
    fun multipleIncrements_countsAccumulate() {
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.onNodeWithTag("level_1").assertRecompositions(atLeast = 3)
        composeTestRule.onNodeWithTag("level_2").assertRecompositions(atLeast = 3)
        composeTestRule.onNodeWithTag("level_3").assertRecompositions(atLeast = 3)
        composeTestRule.onNodeWithTag("level_4").assertRecompositions(atLeast = 3)
        composeTestRule.onNodeWithTag("level_5").assertRecompositions(atLeast = 3)
        composeTestRule.onNodeWithTag("level_6").assertRecompositions(atLeast = 3)
    }

    @Test
    fun level6_deepestLeafTrackedCorrectly() {
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.onNodeWithTag("level_6").assertRecompositions(exactly = 1)
    }

    @Test
    fun siblingChild_neverRecomposes() {
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.onNodeWithTag("sibling_child").assertStable()
    }

    @Test
    fun initialComposition_allLevelsAtZero() {
        composeTestRule.onNodeWithTag("level_1").assertStable()
        composeTestRule.onNodeWithTag("level_2").assertStable()
        composeTestRule.onNodeWithTag("level_3").assertStable()
        composeTestRule.onNodeWithTag("level_4").assertStable()
        composeTestRule.onNodeWithTag("level_5").assertStable()
        composeTestRule.onNodeWithTag("level_6").assertStable()
    }
}
