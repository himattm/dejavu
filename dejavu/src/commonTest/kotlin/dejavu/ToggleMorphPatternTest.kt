package dejavu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Cross-platform port of Android ToggleMorphTest.
 * Validates animation with manual clock (mainClock.autoAdvance = false).
 */
@OptIn(ExperimentalTestApi::class)
class ToggleMorphPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    @Test
    fun toggle_animated_composables_recompose_during_animation() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        mainClock.advanceTimeByFrame()
        waitForIdle()

        onNodeWithTag("toggle_btn").performClick()
        mainClock.advanceTimeBy(200)
        waitForIdle()

        onNodeWithTag("toggle_thumb").assertRecompositions(atLeast = 1)
        onNodeWithTag("toggle_track").assertRecompositions(atLeast = 1)
    }

    @Test
    fun toggle_label_recomposes_once_on_toggle() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("toggle_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun toggle_static_sibling_stays_stable() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_morph_sibling").assertStable()
    }

    @Test
    fun toggle_button_stays_stable() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("toggle_btn").assertStable()
    }

    @Test
    fun toggle_all_animated_parts_recompose() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        mainClock.advanceTimeByFrame()
        waitForIdle()

        onNodeWithTag("toggle_btn").performClick()
        mainClock.advanceTimeBy(200)
        waitForIdle()

        onNodeWithTag("toggle_thumb").assertRecompositions(atLeast = 1)
        onNodeWithTag("toggle_track").assertRecompositions(atLeast = 1)
    }

    @Test
    fun toggle_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()

        resetRecompositionCounts()

        onNodeWithTag("static_morph_sibling").assertStable()
        onNodeWithTag("toggle_thumb").assertStable()
        onNodeWithTag("toggle_track").assertStable()
        onNodeWithTag("toggle_label").assertStable()
        onNodeWithTag("toggle_btn").assertStable()
    }

    @Test
    fun toggle_double_toggle_returns_to_original() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("toggle_label").assertRecompositions(exactly = 2)
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun ToggleMorphScreen() {
    var isOn by remember { mutableStateOf(false) }
    Column(Modifier.testTag("toggle_morph_root")) {
        StaticMorphSibling()
        MorphToggle(isOn)
        ToggleLabel(isOn)
        ToggleButton { isOn = !isOn }
    }
}

@Composable
private fun StaticMorphSibling() {
    BasicText("Static Sibling", Modifier.testTag("static_morph_sibling"))
}

@Composable
private fun ToggleButton(onClick: () -> Unit) {
    BasicText("Toggle", Modifier.testTag("toggle_btn").clickable { onClick() })
}

@Composable
private fun MorphToggle(isOn: Boolean) {
    val thumbPosition by animateFloatAsState(targetValue = if (isOn) 1f else 0f, label = "thumb")
    val trackAlpha by animateFloatAsState(targetValue = if (isOn) 1f else 0.5f, label = "track")
    Column(Modifier.testTag("morph_toggle")) {
        BasicText("Thumb: $thumbPosition", Modifier.testTag("toggle_thumb"))
        BasicText("Track: $trackAlpha", Modifier.testTag("toggle_track"))
    }
}

@Composable
private fun ToggleLabel(isOn: Boolean) {
    BasicText("Label: ${if (isOn) "ON" else "OFF"}", Modifier.testTag("toggle_label"))
}
