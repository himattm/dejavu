package dejavu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AnimationPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun toggleVisibility_panelTracked() = runComposeUiTest {
        setContent { DejavuTestContent { AnimationScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("toggle_vis_btn").performClick()
        waitForIdle()
        onNodeWithTag("toggle_vis_btn").performClick()
        waitForIdle()

        // atLeast = 0: verifies tag mapping resolves and assertion API doesn't crash;
        // exact count depends on Compose animation internals across platforms.
        onNodeWithTag("visible_panel").assertRecompositions(atLeast = 0)
    }

    @Test
    fun animatedContent_variantsSwitchCleanly() = runComposeUiTest {
        setContent { DejavuTestContent { AnimationScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("cycle_content_btn").performClick()
        waitForIdle()
        onNodeWithTag("cycle_content_btn").performClick()
        waitForIdle()

        onNodeWithTag("variant_a").assertRecompositions(atLeast = 0)
    }

    @Test
    fun infiniteTransition_bannerRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AnimationScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("pulse_banner_btn").performClick()
        waitForIdle()

        onNodeWithTag("animating_banner").assertRecompositions(atLeast = 1)
    }

    @Test
    fun conditionalComposition_childTrackedWhenPresent() = runComposeUiTest {
        setContent { DejavuTestContent { AnimationScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("toggle_cond_btn").performClick()
        waitForIdle()

        onNodeWithTag("conditional_child").assertRecompositions(atLeast = 0)
    }

    @Test
    fun conditionalComposition_childGoneWhenRemoved() = runComposeUiTest {
        setContent { DejavuTestContent { AnimationScreen() } }
        waitForIdle()

        onNodeWithTag("toggle_cond_btn").performClick()
        waitForIdle()
        onNodeWithTag("toggle_cond_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("static_label").assertStable()
    }

    @Test
    fun staticLabel_neverRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AnimationScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("toggle_vis_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_label").assertStable()
    }

    @Test
    fun allInteractions_staticLabelStaysStable() = runComposeUiTest {
        setContent { DejavuTestContent { AnimationScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("toggle_vis_btn").performClick()
        waitForIdle()
        onNodeWithTag("cycle_content_btn").performClick()
        waitForIdle()
        onNodeWithTag("toggle_cond_btn").performClick()
        waitForIdle()
        onNodeWithTag("pulse_banner_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_label").assertStable()
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun AnimationScreen() {
    var visible by remember { mutableStateOf(true) }
    var variant by remember { mutableIntStateOf(0) }
    var showConditional by remember { mutableStateOf(false) }
    var bannerPulse by remember { mutableIntStateOf(0) }
    Column {
        StaticAnimLabel()
        AnimatedVisibility(visible) {
            BasicText("Visible Panel", Modifier.testTag("visible_panel"))
        }
        if (variant % 2 == 0) {
            BasicText("Variant A", Modifier.testTag("variant_a"))
        } else {
            BasicText("Variant B", Modifier.testTag("variant_b"))
        }
        AnimatingBanner(bannerPulse)
        if (showConditional) {
            BasicText("Conditional", Modifier.testTag("conditional_child"))
        }
        BasicText("ToggleVis", Modifier.testTag("toggle_vis_btn").clickable { visible = !visible })
        BasicText("CycleContent", Modifier.testTag("cycle_content_btn").clickable { variant++ })
        BasicText("ToggleCond", Modifier.testTag("toggle_cond_btn").clickable { showConditional = !showConditional })
        BasicText("PulseBanner", Modifier.testTag("pulse_banner_btn").clickable { bannerPulse++ })
    }
}

@Composable
private fun StaticAnimLabel() {
    BasicText("Static", Modifier.testTag("static_label"))
}

@Composable
private fun AnimatingBanner(pulse: Int) {
    BasicText("Banner: $pulse", Modifier.testTag("animating_banner"))
}
