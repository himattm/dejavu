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
class AdvancedPatternsTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<AdvancedPatternsActivity>()

    // --- CompositionLocal nesting tests ---

    @Test
    fun nestedLocal_changeOuter_outerReadersRecompose() {
        composeTestRule.onNodeWithTag("change_outer_btn").performClick()
        composeTestRule.onNodeWithTag("outer_reader").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("outer_reader_b").assertRecompositions(exactly = 1)
    }

    @Test
    fun nestedLocal_changeOuter_innerReaderStable() {
        // Inner reader sees the overridden (inner) value, not the outer value
        composeTestRule.onNodeWithTag("change_outer_btn").performClick()
        composeTestRule.onNodeWithTag("inner_reader").assertStable()
    }

    @Test
    fun nestedLocal_changeInner_innerReaderRecomposes() {
        composeTestRule.onNodeWithTag("change_inner_btn").performClick()
        composeTestRule.onNodeWithTag("inner_reader").assertRecompositions(exactly = 1)
    }

    @Test
    fun nestedLocal_changeInner_outerReadersStable() {
        composeTestRule.onNodeWithTag("change_inner_btn").performClick()
        composeTestRule.onNodeWithTag("outer_reader").assertStable()
        composeTestRule.onNodeWithTag("outer_reader_b").assertStable()
    }

    // --- Custom Layout tests ---

    @Test
    fun customLayout_childTrackedCorrectly() {
        // Verify composables inside a custom Layout are trackable
        composeTestRule.onNodeWithTag("custom_layout_child").assertRecompositions(atLeast = 0)
    }

    @Test
    fun customLayout_childStableWhenNoStateChanges() {
        // Trigger an unrelated state change
        composeTestRule.onNodeWithTag("change_outer_btn").performClick()
        composeTestRule.onNodeWithTag("custom_layout_child").assertStable()
    }

    // --- Deferred reads (graphicsLayer) ---

    @Test
    fun deferredRead_toggleVisibility_paramChangeRecomposes() {
        // DeferredReadChild takes `visible: Boolean` as param, so toggling it
        // causes a recomposition (param change), NOT the graphicsLayer
        composeTestRule.onNodeWithTag("toggle_vis_adv_btn").performClick()
        composeTestRule.onNodeWithTag("deferred_read").assertRecompositions(exactly = 1)
    }

    // --- Remember with custom key ---

    @Test
    fun rememberKey_changeKey_childRecomposes() {
        // Changing the key causes remember to recompute, which means
        // the composable that receives the new key param recomposes
        composeTestRule.onNodeWithTag("change_key_btn_adv").performClick()
        composeTestRule.onNodeWithTag("remember_key_child").assertRecompositions(exactly = 1)
    }

    // --- LaunchedEffect restart ---

    @Test
    fun effectRestart_changeEffectKey_childRecomposes() {
        // Changing effectKey causes: parent recomposes -> passes new param to EffectRestartChild
        // -> EffectRestartChild recomposes -> LaunchedEffect restarts -> sets effectRan state
        // -> another recomposition
        composeTestRule.onNodeWithTag("change_effect_key_btn").performClick()
        composeTestRule.onNodeWithTag("effect_restart").assertRecompositions(atLeast = 1)
    }

    @Test
    fun effectRestart_sameKey_childStable() {
        // Wait for the initial LaunchedEffect to fire and settle,
        // then reset counts so the initial effect's state change isn't counted.
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Without changing effectKey, the effect does not restart
        composeTestRule.onNodeWithTag("change_outer_btn").performClick()
        composeTestRule.onNodeWithTag("effect_restart").assertStable()
    }
}
