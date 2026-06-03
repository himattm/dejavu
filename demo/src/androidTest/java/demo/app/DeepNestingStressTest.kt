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
        composeTestRule.waitForIdle()
        // 1: one increment threads `count` through every level, so each recomposes exactly once.
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
        composeTestRule.waitForIdle()
        // 0: sibling subtree is parameterless and never reads `count`, so it stays stable.
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
        composeTestRule.waitForIdle()
        // 3: count threads through every level, so each recomposes once per each of 3 increments.
        composeTestRule.onNodeWithTag("level_1").assertRecompositions(exactly = 3)
        composeTestRule.onNodeWithTag("level_2").assertRecompositions(exactly = 3)
        composeTestRule.onNodeWithTag("level_3").assertRecompositions(exactly = 3)
        composeTestRule.onNodeWithTag("level_4").assertRecompositions(exactly = 3)
        composeTestRule.onNodeWithTag("level_5").assertRecompositions(exactly = 3)
        composeTestRule.onNodeWithTag("level_6").assertRecompositions(exactly = 3)
    }

    @Test
    fun level6_deepestLeafTrackedCorrectly() {
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.waitForIdle()
        // 1: the deepest leaf reads `count`, recomposing exactly once per increment.
        composeTestRule.onNodeWithTag("level_6").assertRecompositions(exactly = 1)
    }

    @Test
    fun siblingChild_neverRecomposes() {
        composeTestRule.onNodeWithTag("deep_inc_btn").performClick()
        composeTestRule.waitForIdle()
        // 0: sibling child never reads `count`, so it never recomposes on increment.
        composeTestRule.onNodeWithTag("sibling_child").assertStable()
    }

    @Test
    fun initialComposition_allLevelsAtZero() {
        // 0: no interaction occurs, so every level is stable (first composition is not a recomposition).
        composeTestRule.onNodeWithTag("level_1").assertStable()
        composeTestRule.onNodeWithTag("level_2").assertStable()
        composeTestRule.onNodeWithTag("level_3").assertStable()
        composeTestRule.onNodeWithTag("level_4").assertStable()
        composeTestRule.onNodeWithTag("level_5").assertStable()
        composeTestRule.onNodeWithTag("level_6").assertStable()
    }
}
