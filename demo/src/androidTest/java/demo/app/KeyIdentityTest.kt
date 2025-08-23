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
class KeyIdentityTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<KeyIdentityActivity>()

    @Test
    fun keyChange_treatedAsFreshComposition() {
        // First recompose keyed child to establish some count
        composeTestRule.onNodeWithTag("keyed_local_btn").performClick()
        composeTestRule.waitForIdle()

        // Reset counts, then change key — parent recomposes, creating new keyed child
        composeTestRule.resetRecompositionCounts()
        composeTestRule.onNodeWithTag("change_key_btn").performClick()

        // Dejavu tracks by qualified name, so key change appears as 1 recomposition
        // (parent recomposition propagates to the new child instance)
        composeTestRule.onNodeWithTag("keyed_child").assertRecompositions(exactly = 1)
    }

    @Test
    fun keyChange_thenLocalInc_countsFromZero() {
        // Change key to get fresh identity
        composeTestRule.onNodeWithTag("change_key_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Increment local state in the new keyed child
        composeTestRule.onNodeWithTag("keyed_local_btn").performClick()
        composeTestRule.onNodeWithTag("keyed_child").assertRecompositions(exactly = 1)
    }

    @Test
    fun saveableState_recomposesLikeMutableState() {
        composeTestRule.onNodeWithTag("saveable_inc_btn").performClick()
        composeTestRule.onNodeWithTag("saveable_child").assertRecompositions(exactly = 1)
    }

    @Test
    fun saveableState_multipleIncrements() {
        repeat(3) {
            composeTestRule.onNodeWithTag("saveable_inc_btn").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("saveable_child").assertRecompositions(exactly = 3)
    }

    @Test
    fun loopItems_initiallyStable() {
        composeTestRule.onNodeWithTag("loop_item_0").assertStable()
        composeTestRule.onNodeWithTag("loop_item_1").assertStable()
        composeTestRule.onNodeWithTag("loop_item_2").assertStable()
    }

    @Test
    fun addLoopItem_existingItemsShareCountWithNewItem() {
        composeTestRule.onNodeWithTag("add_loop_btn").performClick()
        // All LoopItem instances share the same qualified name in Dejavu's tracker.
        // The new item's initial composition registers as 1 recomposition for all tags
        // that resolve to LoopItem, even though existing items were actually skipped.
        composeTestRule.onNodeWithTag("loop_item_0").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("loop_item_1").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("loop_item_2").assertRecompositions(exactly = 1)
    }

    @Test
    fun addLoopItem_countLabelRecomposes() {
        composeTestRule.onNodeWithTag("add_loop_btn").performClick()
        composeTestRule.onNodeWithTag("loop_count_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun derivedChainA_recomposesWhenIntegerDivisionChanges() {
        // baseValue 0→1: derivedA = 0/2=0 → 1/2=0 (no change due to int division)
        composeTestRule.onNodeWithTag("inc_base_btn").performClick()
        composeTestRule.onNodeWithTag("derived_chain_a").assertStable()

        // Reset, then baseValue 1→2: derivedA = 1/2=0 → 2/2=1 (changes!)
        composeTestRule.resetRecompositionCounts()
        composeTestRule.onNodeWithTag("inc_base_btn").performClick()
        composeTestRule.onNodeWithTag("derived_chain_a").assertRecompositions(exactly = 1)
    }

    @Test
    fun derivedChainB_recomposesOnlyOnBooleanFlip() {
        // baseValue 0→2: derivedA 0→1, derivedB false→true (flips!)
        composeTestRule.onNodeWithTag("inc_base_twice_btn").performClick()
        composeTestRule.onNodeWithTag("derived_chain_b").assertRecompositions(exactly = 1)

        // Reset, then baseValue 2→4: derivedA 1→2, derivedB stays true (no flip)
        composeTestRule.resetRecompositionCounts()
        composeTestRule.onNodeWithTag("inc_base_twice_btn").performClick()
        composeTestRule.onNodeWithTag("derived_chain_b").assertStable()
    }

    @Test
    fun derivedChain_intermediateNoChange_leafStable() {
        // baseValue 0→1: derivedA = 0/2=0 → 1/2=0 (unchanged), so derivedB also unchanged
        composeTestRule.onNodeWithTag("inc_base_btn").performClick()
        composeTestRule.onNodeWithTag("derived_chain_a").assertStable()
        composeTestRule.onNodeWithTag("derived_chain_b").assertStable()
    }
}
