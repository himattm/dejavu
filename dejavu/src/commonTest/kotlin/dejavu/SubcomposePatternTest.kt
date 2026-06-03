package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises Compose subcomposition (`BoxWithConstraints`/`SubcomposeLayout`), a
 * `@NonRestartableComposable`, and `movableContentOf` — verifying Dejavu's recomposition
 * counts against a [GroundTruth] `SideEffect` (the runtime runs the effect after every
 * successful composition, so it is the real composition count).
 *
 * Subcomposition note: `BoxWithConstraints` composes [ConstraintReader] inside a child
 * composition. The public per-tag API resolves counts across that subcomposition boundary, so
 * [ConstraintReader] is classified by the same rules as any other node.
 *
 * Instance classification used below — every tracked composable here is **single-instance** with a
 * unique testTag (no loops, no multi-tag composables), so per-tag counts resolve exactly on all
 * platforms → `exactly = GroundTruth.delta(tag)`:
 * - `constraint_reader` ([ConstraintReader], subcomposed via `BoxWithConstraints`)
 * - `regular_child` ([RegularChild])
 * - `non_restartable` ([NonRestartableChild], `@NonRestartableComposable`)
 * - `movable_child` ([MovableChild], hoisted via `movableContentOf`)
 *
 * Every test calls [resetRecompositionCounts] + [GroundTruth.snapshotBaseline] after the initial
 * `waitForIdle()` (or after pre-interactions), keeping `delta`/the tracer aligned at the zero point.
 */
@OptIn(ExperimentalTestApi::class)
class SubcomposePatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun constraintChange_recomposesSubcomposition() = runComposeUiTest {
        setContent { DejavuTestContent { SubcomposeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Toggling the Box width changes BoxWithConstraints' incoming constraints, so the
        // subcomposed ConstraintReader recomposes with a new maxWidth param.
        onNodeWithTag("toggle_width_btn").performClick()
        waitForIdle()

        onNodeWithTag("constraint_reader")
            .assertRecompositions(exactly = GroundTruth.delta("constraint_reader"))
        assertEquals(1, GroundTruth.delta("constraint_reader"), "width change recomposes the subcomposed reader once")
    }

    @Test
    fun noConstraintChange_subcompositionStable() = runComposeUiTest {
        setContent { DejavuTestContent { SubcomposeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Bumping `trigger` recomposes the parent but does not change the Box width, so the
        // subcomposition's constraints are unchanged and ConstraintReader must stay stable.
        onNodeWithTag("trigger_parent_btn").performClick()
        waitForIdle()

        onNodeWithTag("constraint_reader")
            .assertRecompositions(exactly = GroundTruth.delta("constraint_reader"))
        assertEquals(0, GroundTruth.delta("constraint_reader"), "unchanged constraints must not recompose the subcomposition")
    }

    @Test
    fun moveContent_preservesState_noRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { MovableScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Moving movable content from slot A to slot B relocates the same composition instance;
        // movableContentOf preserves it, so the child does not recompose.
        onNodeWithTag("move_content_btn").performClick()
        waitForIdle()

        onNodeWithTag("movable_child")
            .assertRecompositions(exactly = GroundTruth.delta("movable_child"))
        assertEquals(0, GroundTruth.delta("movable_child"), "moving content preserves the instance without recomposing it")
    }

    @Test
    fun moveContentTwice_backToOriginalSlot_stillStable() = runComposeUiTest {
        setContent { DejavuTestContent { MovableScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Two moves (A→B→A) round-trip the same instance back to its origin; it stays preserved
        // throughout, so the child never recomposes.
        onNodeWithTag("move_content_btn").performClick()
        waitForIdle()
        onNodeWithTag("move_content_btn").performClick()
        waitForIdle()

        onNodeWithTag("movable_child")
            .assertRecompositions(exactly = GroundTruth.delta("movable_child"))
        assertEquals(0, GroundTruth.delta("movable_child"), "round-tripping content preserves the instance without recomposing it")
    }

    @Test
    fun nonRestartable_recomposesWithParent() = runComposeUiTest {
        setContent { DejavuTestContent { SubcomposeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // A @NonRestartableComposable cannot recompose on its own; it recomposes only when its
        // caller does. Bumping `trigger` recomposes the parent, which re-runs NonRestartableChild
        // with a new param → exactly one recomposition, matching the runtime's SideEffect.
        onNodeWithTag("trigger_parent_btn").performClick()
        waitForIdle()

        onNodeWithTag("non_restartable")
            .assertRecompositions(exactly = GroundTruth.delta("non_restartable"))
        assertEquals(1, GroundTruth.delta("non_restartable"), "non-restartable child recomposes once with its parent")
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun SubcomposeScreen() {
    var wide by remember { mutableStateOf(false) }
    var trigger by remember { mutableIntStateOf(0) }
    Column {
        Box(Modifier.let { if (wide) it.fillMaxWidth() else it.width(100.dp) }) {
            BoxWithConstraints {
                ConstraintReader(maxWidth.value.toInt())
            }
        }
        RegularChild(trigger)
        NonRestartableChild(trigger)
        BasicText("ToggleWidth", Modifier.testTag("toggle_width_btn").clickable { wide = !wide })
        BasicText("TriggerParent", Modifier.testTag("trigger_parent_btn").clickable { trigger++ })
    }
}

@Composable
private fun ConstraintReader(maxWidthPx: Int) {
    SideEffect { GroundTruth.record("constraint_reader") }
    BasicText("Width: $maxWidthPx", Modifier.testTag("constraint_reader"))
}

@Composable
private fun RegularChild(value: Int) {
    SideEffect { GroundTruth.record("regular_child") }
    BasicText("Regular: $value", Modifier.testTag("regular_child"))
}

@NonRestartableComposable
@Composable
private fun NonRestartableChild(value: Int) {
    SideEffect { GroundTruth.record("non_restartable") }
    BasicText("NonRestart: $value", Modifier.testTag("non_restartable"))
}

@Composable
private fun MovableScreen() {
    var inSlotA by remember { mutableStateOf(true) }
    val movable = remember { movableContentOf { MovableChild() } }
    Column {
        if (inSlotA) {
            Box(Modifier.testTag("slot_a")) { movable() }
        } else {
            Box(Modifier.testTag("slot_b")) { movable() }
        }
        BasicText("Move", Modifier.testTag("move_content_btn").clickable { inSlotA = !inSlotA })
    }
}

@Composable
private fun MovableChild() {
    SideEffect { GroundTruth.record("movable_child") }
    BasicText("Movable", Modifier.testTag("movable_child"))
}
