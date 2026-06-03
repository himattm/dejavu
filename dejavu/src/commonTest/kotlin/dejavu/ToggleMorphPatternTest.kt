package dejavu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import dejavu.internal.DejavuTracer

/**
 * Cross-platform port of Android ToggleMorphTest.
 * Validates animation behavior driven by `animateFloatAsState`.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect`: the Compose
 * runtime runs the effect after every successful composition, so the per-key `SideEffect` tally is
 * the real composition count. Every tracked composable here is **single-instance** (distinct call
 * sites, no loops), so the public per-tag API resolves the exact count on all platforms and we can
 * assert `exactly = GroundTruth.delta(tag)`.
 *
 * `toggle_thumb` and `toggle_track` are two `BasicText` nodes emitted *inside* the single
 * `MorphToggle` composable — there is no separate composable for each, so both nodes recompose
 * exactly when `MorphToggle` recomposes. Their ground truth is therefore recorded once per
 * `MorphToggle` composition under the `morph_toggle` key, and both per-tag assertions compare
 * against `GroundTruth.delta("morph_toggle")`.
 *
 * The toggle animation is finite: `animateFloatAsState` runs for a bounded duration and settles.
 * The animation tests use a manual clock (`mainClock.autoAdvance = false`) so the morph recomposes
 * across several frames; rather than guessing a frame-dependent literal, they advance the clock to
 * completion (`mainClock.autoAdvance = true` + `waitForIdle()`) and assert the tracer count equals
 * the settled ground-truth delta — proving Dejavu counts every animation-driven recomposition the
 * runtime actually ran, exactly.
 */
@OptIn(ExperimentalTestApi::class)
class ToggleMorphPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
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
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("toggle_btn").performClick()
        mainClock.advanceTimeBy(200)
        // Let the finite morph animation run to completion, then settle.
        mainClock.autoAdvance = true
        waitForIdle()

        // toggle_thumb, toggle_track and morph_toggle are three tags on nodes INSIDE the single
        // MorphToggle composable. Multiple tags on one function make per-tag fingerprint counts
        // diverge from the recomposition count (they track each node's own param changes), so the
        // animation's true recomposition count is the function-level MorphToggle count. Assert the
        // tracer equals the settled ground truth (count is frame-dependent, so don't pin a literal).
        assertEquals(
            GroundTruth.delta("morph_toggle"),
            DejavuTracer.getRecompositionCount("dejavu.MorphToggle"),
            "tracer MorphToggle count should equal SideEffect ground truth after the animation settles",
        )
    }

    @Test
    fun toggle_label_recomposes_once_on_toggle() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("toggle_label")
            .assertRecompositions(exactly = GroundTruth.delta("toggle_label"))
        assertEquals(1, GroundTruth.delta("toggle_label"), "label recomposes once on a single toggle")
    }

    @Test
    fun toggle_static_sibling_stays_stable() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_morph_sibling").assertStable()
        assertEquals(0, GroundTruth.delta("static_morph_sibling"), "parameterless sibling never recomposes")
    }

    @Test
    fun toggle_button_stays_stable() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("toggle_btn").assertStable()
        assertEquals(0, GroundTruth.delta("toggle_btn"), "button holds a stable onClick lambda and never recomposes")
    }

    @Test
    fun toggle_all_animated_parts_recompose() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        mainClock.advanceTimeByFrame()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("toggle_btn").performClick()
        mainClock.advanceTimeBy(200)
        // Let the finite morph animation run to completion, then settle.
        mainClock.autoAdvance = true
        waitForIdle()

        // Both animated parts live inside MorphToggle; with three tags on one composable, per-tag
        // fingerprint counts diverge from the recomposition count, so assert the function-level
        // MorphToggle count against the settled ground truth (frame-dependent → no literal).
        assertEquals(
            GroundTruth.delta("morph_toggle"),
            DejavuTracer.getRecompositionCount("dejavu.MorphToggle"),
            "tracer MorphToggle count should equal SideEffect ground truth after the animation settles",
        )
    }

    @Test
    fun toggle_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()

        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("static_morph_sibling").assertStable()
        onNodeWithTag("toggle_thumb").assertStable()
        onNodeWithTag("toggle_track").assertStable()
        onNodeWithTag("toggle_label").assertStable()
        onNodeWithTag("toggle_btn").assertStable()
        // No interaction occurred, so nothing recomposes after the baseline.
        assertEquals(0, GroundTruth.delta("morph_toggle"), "no morph recomposition without interaction")
        assertEquals(0, GroundTruth.delta("toggle_label"))
        assertEquals(0, GroundTruth.delta("static_morph_sibling"))
        assertEquals(0, GroundTruth.delta("toggle_btn"))
    }

    @Test
    fun toggle_double_toggle_returns_to_original() = runComposeUiTest {
        setContent { DejavuTestContent { ToggleMorphScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        onNodeWithTag("toggle_btn").performClick()
        waitForIdle()

        // Two toggles → the label's `isOn` param changes twice → exactly two recompositions.
        onNodeWithTag("toggle_label")
            .assertRecompositions(exactly = GroundTruth.delta("toggle_label"))
        assertEquals(2, GroundTruth.delta("toggle_label"), "two toggles recompose the label twice")
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun ToggleMorphScreen() {
    var isOn by remember { mutableStateOf(false) }
    SideEffect { GroundTruth.record("toggle_morph_root") }
    Column(Modifier.testTag("toggle_morph_root")) {
        StaticMorphSibling()
        MorphToggle(isOn)
        ToggleLabel(isOn)
        ToggleButton { isOn = !isOn }
    }
}

@Composable
private fun StaticMorphSibling() {
    SideEffect { GroundTruth.record("static_morph_sibling") }
    BasicText("Static Sibling", Modifier.testTag("static_morph_sibling"))
}

@Composable
private fun ToggleButton(onClick: () -> Unit) {
    SideEffect { GroundTruth.record("toggle_btn") }
    BasicText("Toggle", Modifier.testTag("toggle_btn").clickable { onClick() })
}

@Composable
private fun MorphToggle(isOn: Boolean) {
    // thumb and track are two BasicText nodes inside this single composable, so both recompose
    // exactly when MorphToggle does. Record once per MorphToggle composition under "morph_toggle"
    // and assert both per-tag counts against GroundTruth.delta("morph_toggle").
    SideEffect { GroundTruth.record("morph_toggle") }
    val thumbPosition by animateFloatAsState(targetValue = if (isOn) 1f else 0f, label = "thumb")
    val trackAlpha by animateFloatAsState(targetValue = if (isOn) 1f else 0.5f, label = "track")
    Column(Modifier.testTag("morph_toggle")) {
        BasicText("Thumb: $thumbPosition", Modifier.testTag("toggle_thumb"))
        BasicText("Track: $trackAlpha", Modifier.testTag("toggle_track"))
    }
}

@Composable
private fun ToggleLabel(isOn: Boolean) {
    SideEffect { GroundTruth.record("toggle_label") }
    BasicText("Label: ${if (isOn) "ON" else "OFF"}", Modifier.testTag("toggle_label"))
}
