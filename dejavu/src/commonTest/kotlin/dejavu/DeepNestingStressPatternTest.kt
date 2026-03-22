package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * Cross-platform port of Android DeepNestingStressTest.
 * Validates 6-level nesting hierarchy with sibling isolation.
 */
@OptIn(ExperimentalTestApi::class)
class DeepNestingStressPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    @Test
    fun increment_allLevelsRecompose() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()

        onNodeWithTag("deep_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("level_1").assertRecompositions(exactly = 1)
        onNodeWithTag("level_2").assertRecompositions(exactly = 1)
        onNodeWithTag("level_3").assertRecompositions(exactly = 1)
        onNodeWithTag("level_4").assertRecompositions(exactly = 1)
        onNodeWithTag("level_5").assertRecompositions(exactly = 1)
        onNodeWithTag("level_6").assertRecompositions(exactly = 1)
    }

    @Test
    fun increment_siblingBranchStaysStable() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()

        onNodeWithTag("deep_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("sibling_branch").assertStable()
        onNodeWithTag("sibling_child").assertStable()
    }

    @Test
    fun multipleIncrements_countsAccumulate() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()

        repeat(3) {
            onNodeWithTag("deep_inc_btn").performClick()
            waitForIdle()
        }

        onNodeWithTag("level_1").assertRecompositions(atLeast = 3)
        onNodeWithTag("level_2").assertRecompositions(atLeast = 3)
        onNodeWithTag("level_3").assertRecompositions(atLeast = 3)
        onNodeWithTag("level_4").assertRecompositions(atLeast = 3)
        onNodeWithTag("level_5").assertRecompositions(atLeast = 3)
        onNodeWithTag("level_6").assertRecompositions(atLeast = 3)
    }

    @Test
    fun level6_deepestLeafTrackedCorrectly() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()

        onNodeWithTag("deep_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("level_6").assertRecompositions(exactly = 1)
    }

    @Test
    fun siblingChild_neverRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()

        onNodeWithTag("deep_inc_btn").performClick()
        waitForIdle()

        onNodeWithTag("sibling_child").assertStable()
    }

    @Test
    fun initialComposition_allLevelsAtZero() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()

        onNodeWithTag("level_1").assertStable()
        onNodeWithTag("level_2").assertStable()
        onNodeWithTag("level_3").assertStable()
        onNodeWithTag("level_4").assertStable()
        onNodeWithTag("level_5").assertStable()
        onNodeWithTag("level_6").assertStable()
    }

    @Test
    fun siblingBranch_stableAfterMultipleIncrements() = runComposeUiTest {
        setContent { DejavuTestContent { DeepNestingScreen() } }
        waitForIdle()

        repeat(3) {
            onNodeWithTag("deep_inc_btn").performClick()
            waitForIdle()
        }

        onNodeWithTag("sibling_branch").assertStable()
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
     * should reflect the recomposition.
     */
    @Test
    fun singleInstanceWithMultipleTags_functionLevelTracking() = runComposeUiTest {
        setContent { DejavuTestContent { MultiTagScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("multi_tag_btn").performClick()
        waitForIdle()

        // Verify recomposition is tracked (via function-level count since both tags
        // map to MultiTagScreen)
        onNodeWithTag("multi_tag_btn").assertRecompositions(atLeast = 1)
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
    Column(Modifier.testTag("level_1")) { BasicText("L1: $value"); Level2(value) }
}

@Composable
private fun Level2(value: Int) {
    Column(Modifier.testTag("level_2")) { BasicText("L2: $value"); Level3(value) }
}

@Composable
private fun Level3(value: Int) {
    Column(Modifier.testTag("level_3")) { BasicText("L3: $value"); Level4(value) }
}

@Composable
private fun Level4(value: Int) {
    Column(Modifier.testTag("level_4")) { BasicText("L4: $value"); Level5(value) }
}

@Composable
private fun Level5(value: Int) {
    Column(Modifier.testTag("level_5")) { BasicText("L5: $value"); Level6(value) }
}

@Composable
private fun Level6(value: Int) {
    BasicText("L6: $value", Modifier.testTag("level_6"))
}

@Composable
private fun SiblingBranch() {
    Column(Modifier.testTag("sibling_branch")) { SiblingChild() }
}

@Composable
private fun SiblingChild() {
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
    Column(Modifier.testTag("multi_tag_root")) {
        BasicText("Count: $count")
        BasicText("Inc", Modifier.testTag("multi_tag_btn").clickable { count++ })
    }
}
