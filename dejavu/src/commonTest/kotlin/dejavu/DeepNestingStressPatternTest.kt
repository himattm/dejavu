package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * Cross-platform port of Android DeepNestingStressTest.
 * Validates 6-level nesting hierarchy with sibling isolation.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect`. Every level
 * (`Level1`..`Level6`), the sibling branch, and the sibling child are **single-instance** composables
 * with unique composer keys (distinct call sites), so the public per-tag API resolves their exact
 * count on all platforms → `exactly = GroundTruth.delta(tag)`. Because the increment threads a single
 * `count` parameter through every level, each level recomposes exactly once per increment, which the
 * tests assert deterministically; the parameterless sibling subtree stays stable.
 *
 * Each test calls [resetRecompositionCounts] + [GroundTruth.snapshotBaseline] after the initial
 * `waitForIdle()` (and after any pre-interaction), keeping `delta`/tracer aligned at the zero point.
 */
@OptIn(ExperimentalTestApi::class)
class DeepNestingStressPatternTest {

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
    fun increment_allLevelsRecompose() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("deep_inc_btn").performClick()
        waitForIdle()

        // A single increment threads `count` through every level: each recomposes exactly once.
        onNodeWithTag("level_1").assertRecompositions(exactly = GroundTruth.delta("level_1"))
        onNodeWithTag("level_2").assertRecompositions(exactly = GroundTruth.delta("level_2"))
        onNodeWithTag("level_3").assertRecompositions(exactly = GroundTruth.delta("level_3"))
        onNodeWithTag("level_4").assertRecompositions(exactly = GroundTruth.delta("level_4"))
        onNodeWithTag("level_5").assertRecompositions(exactly = GroundTruth.delta("level_5"))
        onNodeWithTag("level_6").assertRecompositions(exactly = GroundTruth.delta("level_6"))
        assertEquals(1, GroundTruth.delta("level_1"), "level 1 recomposes once per increment")
        assertEquals(1, GroundTruth.delta("level_2"), "level 2 recomposes once per increment")
        assertEquals(1, GroundTruth.delta("level_3"), "level 3 recomposes once per increment")
        assertEquals(1, GroundTruth.delta("level_4"), "level 4 recomposes once per increment")
        assertEquals(1, GroundTruth.delta("level_5"), "level 5 recomposes once per increment")
        assertEquals(1, GroundTruth.delta("level_6"), "level 6 recomposes once per increment")
    }

    @Test
    fun increment_siblingBranchStaysStable() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("deep_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("sibling_branch").assertStable()
        onNodeWithTag("sibling_child").assertStable()
        assertEquals(0, GroundTruth.delta("sibling_branch"), "sibling branch is parameterless")
        assertEquals(0, GroundTruth.delta("sibling_child"), "sibling child is parameterless")
    }

    @Test
    fun multipleIncrements_countsAccumulate() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        repeat(3) {
            onNodeWithTag("deep_inc_btn").performClick()
            waitForIdle()
        }

        // Three increments → each level recomposes once per increment → 3 total each.
        onNodeWithTag("level_1").assertRecompositions(exactly = GroundTruth.delta("level_1"))
        onNodeWithTag("level_2").assertRecompositions(exactly = GroundTruth.delta("level_2"))
        onNodeWithTag("level_3").assertRecompositions(exactly = GroundTruth.delta("level_3"))
        onNodeWithTag("level_4").assertRecompositions(exactly = GroundTruth.delta("level_4"))
        onNodeWithTag("level_5").assertRecompositions(exactly = GroundTruth.delta("level_5"))
        onNodeWithTag("level_6").assertRecompositions(exactly = GroundTruth.delta("level_6"))
        assertEquals(3, GroundTruth.delta("level_1"), "level 1 recomposes once per each of 3 increments")
        assertEquals(3, GroundTruth.delta("level_6"), "level 6 recomposes once per each of 3 increments")
    }

    @Test
    fun level6_deepestLeafTrackedCorrectly() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("deep_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("level_6").assertRecompositions(exactly = GroundTruth.delta("level_6"))
        assertEquals(1, GroundTruth.delta("level_6"), "deepest leaf recomposes once per increment")
    }

    @Test
    fun siblingChild_neverRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("deep_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("sibling_child").assertStable()
        assertEquals(0, GroundTruth.delta("sibling_child"), "sibling child never recomposes on increment")
    }

    @Test
    fun initialComposition_allLevelsAtZero() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No interaction: every node must be stable.
        onNodeWithTag("level_1").assertStable()
        onNodeWithTag("level_2").assertStable()
        onNodeWithTag("level_3").assertStable()
        onNodeWithTag("level_4").assertStable()
        onNodeWithTag("level_5").assertStable()
        onNodeWithTag("level_6").assertStable()
        assertEquals(0, GroundTruth.delta("level_1"))
        assertEquals(0, GroundTruth.delta("level_2"))
        assertEquals(0, GroundTruth.delta("level_3"))
        assertEquals(0, GroundTruth.delta("level_4"))
        assertEquals(0, GroundTruth.delta("level_5"))
        assertEquals(0, GroundTruth.delta("level_6"))
    }

    @Test
    fun siblingBranch_stableAfterMultipleIncrements() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        repeat(3) {
            onNodeWithTag("deep_inc_btn").performClick()
            waitForIdle()
        }

        onNodeWithTag("sibling_branch").assertStable()
        assertEquals(0, GroundTruth.delta("sibling_branch"), "sibling branch stays stable across increments")
    }

    // ── Per-tag tracking regression test (port of Android PerTagTrackingRegressionTest fix 1) ──
    // Note: the Android version of this test uses Group.identity (Anchor) to detect
    // single-instance vs multi-instance composables. CommonTagMapping assigns identity
    // per-CompositionGroup (not per composable instance), so the Android-specific
    // identity-based multi-instance detection test is not portable. Instead, we verify
    // that a single-instance composable with multiple tags tracks recomposition via
    // function-level counting.

    /**
     * A single composable with multiple tags should still track recomposition.
     * After clicking the button, the function-level count for MultiTagScreen
     * should reflect the recomposition. `MultiTagScreen` is single-instance, so both
     * tags map to the same function and `delta(tag)` lines up with the per-tag assertion.
     */
    @Test
    fun singleInstanceWithMultipleTags_functionLevelTracking() = runComposeUiTest {
        setContent { DejavuTestContent { MultiTagScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("multi_tag_btn").performClick()
        waitForIdle()

        // Both tags map to single-instance MultiTagScreen, so the per-tag count resolves to the
        // function-level count exactly — assert tracer == ground truth rather than a loose bound.
        onNodeWithTag("multi_tag_btn")
            .assertRecompositions(exactly = GroundTruth.delta("multi_tag_btn"))
        assertEquals(1, GroundTruth.delta("multi_tag_btn"), "screen recomposes once on count change")
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun DeepNestingScreen() {
    var count by remember { mutableIntStateOf(0) }
    Column {
        Level1(count)
        SiblingBranch()
        BasicText("Inc", Modifier.testTag("deep_inc_btn").clickable { count++ })
    }
}

@Composable
private fun Level1(value: Int) {
    SideEffect { GroundTruth.record("level_1") }
    Column(Modifier.testTag("level_1")) { BasicText("L1: $value"); Level2(value) }
}

@Composable
private fun Level2(value: Int) {
    SideEffect { GroundTruth.record("level_2") }
    Column(Modifier.testTag("level_2")) { BasicText("L2: $value"); Level3(value) }
}

@Composable
private fun Level3(value: Int) {
    SideEffect { GroundTruth.record("level_3") }
    Column(Modifier.testTag("level_3")) { BasicText("L3: $value"); Level4(value) }
}

@Composable
private fun Level4(value: Int) {
    SideEffect { GroundTruth.record("level_4") }
    Column(Modifier.testTag("level_4")) { BasicText("L4: $value"); Level5(value) }
}

@Composable
private fun Level5(value: Int) {
    SideEffect { GroundTruth.record("level_5") }
    Column(Modifier.testTag("level_5")) { BasicText("L5: $value"); Level6(value) }
}

@Composable
private fun Level6(value: Int) {
    SideEffect { GroundTruth.record("level_6") }
    BasicText("L6: $value", Modifier.testTag("level_6"))
}

@Composable
private fun SiblingBranch() {
    SideEffect { GroundTruth.record("sibling_branch") }
    Column(Modifier.testTag("sibling_branch")) { SiblingChild() }
}

@Composable
private fun SiblingChild() {
    SideEffect { GroundTruth.record("sibling_child") }
    BasicText("Sibling", Modifier.testTag("sibling_child"))
}

/**
 * A composable with multiple testTags on a single instance. The root Column
 * is tagged "multi_tag_root" and the button inside is tagged "multi_tag_btn".
 * Both tags map to MultiTagScreen, but they share one Group identity.
 */
@Composable
private fun MultiTagScreen() {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { GroundTruth.record("multi_tag_btn") }
    Column(Modifier.testTag("multi_tag_root")) {
        BasicText("Count: $count")
        BasicText("Inc", Modifier.testTag("multi_tag_btn").clickable { count++ })
    }
}
