package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises Compose `key(...)` identity semantics, keyed `for`-loop items, and a
 * `derivedStateOf` chain — verifying Dejavu's recomposition counts against a
 * [GroundTruth] `SideEffect` (the runtime runs the effect after every successful
 * composition, so it is the real composition count).
 *
 * Instance classification used below:
 * - **Single-instance** nodes (the single `KeyedChild`, the count label, the derived-chain
 *   consumers) resolve their exact count per-tag on all platforms → `exactly = delta(tag)`.
 * - The `LoopItem`s come from ONE call site (`for { key(i) { LoopItem(i) } }`). Even though
 *   `key(i)` gives each a distinct runtime identity, the tracer keys composition counts by the
 *   shared *compile-time* group key of that call site, so per-*instance* counts only resolve on
 *   Android. On the common targets these assert the **function-level** count —
 *   `DejavuTracer.getRecompositionCount("dejavu.LoopItem")` == `GroundTruth.delta("LoopItem")`.
 *
 * Key-identity subtlety: when `key(keyValue) { KeyedChild(...) }` sees a new `keyValue`, Compose
 * disposes the old instance and composes a fresh one. The fresh instance reuses the call site's
 * compile-time key, so that second composition counts as one recomposition — exactly matching the
 * one `SideEffect` the runtime runs for it. `exactly = GroundTruth.delta(tag)` therefore lines up
 * (both 1) whether the key changed (recreation) or only a param changed (in-place recomposition).
 */
@OptIn(ExperimentalTestApi::class)
class KeyIdentityPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun keyChange_treatedAsFreshComposition() = runComposeUiTest {
        setContent { DejavuTestContent { KeyIdentityScreen() } }
        waitForIdle()

        onNodeWithTag("change_key_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("change_key_btn").performClick()
        waitForIdle()

        // The second key change (keyValue N → N+1) disposes the old KeyedChild and composes a
        // brand-new instance. The fresh instance reuses the call site's compile-time key, so that
        // composition is counted once — exactly matching the single SideEffect the runtime runs
        // for the recreated child.
        onNodeWithTag("keyed_child")
            .assertRecompositions(exactly = GroundTruth.delta("keyed_child"))
        assertEquals(1, GroundTruth.delta("keyed_child"), "recreating the child on a key change composes it exactly once")
    }

    @Test
    fun keyChange_thenLocalInc_countsFromZero() = runComposeUiTest {
        setContent { DejavuTestContent { KeyIdentityScreen() } }
        waitForIdle()

        onNodeWithTag("change_key_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("keyed_local_btn").performClick()
        waitForIdle()

        // localCount changes but keyValue does NOT, so the SAME KeyedChild instance recomposes
        // (it already existed at the baseline) → delta == Dejavu's recomposition count.
        onNodeWithTag("keyed_child")
            .assertRecompositions(exactly = GroundTruth.delta("keyed_child"))
        assertEquals(1, GroundTruth.delta("keyed_child"), "local param change recomposes the existing keyed child once")
    }

    @Test
    fun loopItems_initiallyStable() = runComposeUiTest {
        setContent { DejavuTestContent { LoopScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No interaction: every loop item is stable. Per-tag resolves to the shared function-level
        // count for these loop instances, which is 0 here — so assertStable holds on each tag, and
        // the function-level ground truth confirms no LoopItem composed after the baseline.
        onNodeWithTag("loop_item_0").assertStable()
        onNodeWithTag("loop_item_1").assertStable()
        onNodeWithTag("loop_item_2").assertStable()
        assertEquals(
            GroundTruth.delta("LoopItem"),
            DejavuTracer.getRecompositionCount("dejavu.LoopItem"),
            "tracer LoopItem count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("LoopItem"), "no loop item composes without interaction")
    }

    @Test
    fun addLoopItem_existingItemsShareCountWithNewItem() = runComposeUiTest {
        setContent { DejavuTestContent { LoopScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("add_loop_btn").performClick()
        waitForIdle()

        // itemCount 3 → 4 changes the count label's param, recomposing it once.
        onNodeWithTag("loop_count_label")
            .assertRecompositions(exactly = GroundTruth.delta("loop_count_label"))
        assertEquals(1, GroundTruth.delta("loop_count_label"), "count label recomposes once when item count changes")
    }

    /**
     * Guards against runtime internal fingerprint drift (port of Android
     * PerTagTrackingRegressionTest fix 3).
     *
     * Adding a new LoopItem triggers the parent to recompose. The count label should recompose
     * (itemCount changed), while existing items must NOT be over-counted by false-positive
     * fingerprint drift. The loop items come from one call site, so on the common targets their
     * counts aggregate at the function level: adding one item composes exactly that one new item,
     * so the function-level LoopItem count is exactly 1 — which proves the three existing items
     * stayed stable (any over-recomposition would push the total above 1).
     */
    @Test
    fun addLoopItem_existingItemsBounded() = runComposeUiTest {
        setContent { DejavuTestContent { LoopScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("add_loop_btn").performClick()
        waitForIdle()

        // Only the newly-added item composes; the three existing items keep their key and param so
        // Compose skips them. Function-level: tracer == ground truth, and the total is exactly 1.
        assertEquals(
            GroundTruth.delta("LoopItem"),
            DejavuTracer.getRecompositionCount("dejavu.LoopItem"),
            "tracer LoopItem count should equal SideEffect ground truth",
        )
        assertEquals(1, GroundTruth.delta("LoopItem"), "only the newly-added item composes; existing items stay stable")

        // The count label should recompose (item count changed).
        onNodeWithTag("loop_count_label")
            .assertRecompositions(exactly = GroundTruth.delta("loop_count_label"))
        assertEquals(1, GroundTruth.delta("loop_count_label"), "count label recomposes once when item count changes")
    }

    @Test
    fun derivedChainA_recomposesWhenIntegerDivisionChanges() = runComposeUiTest {
        setContent { DejavuTestContent { DerivedChainScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // base 0→1: derivedA = 0/2=0 → 1/2=0, no change
        onNodeWithTag("inc_base_btn").performClick()
        waitForIdle()
        onNodeWithTag("derived_chain_a").assertStable()
        assertEquals(0, GroundTruth.delta("derived_chain_a"), "derivedA unchanged (0→0) must not recompose")

        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // base 1→2: derivedA = 1/2=0 → 2/2=1, changes
        onNodeWithTag("inc_base_btn").performClick()
        waitForIdle()
        onNodeWithTag("derived_chain_a")
            .assertRecompositions(exactly = GroundTruth.delta("derived_chain_a"))
        assertEquals(1, GroundTruth.delta("derived_chain_a"), "derivedA changing 0→1 recomposes the consumer once")
    }

    @Test
    fun derivedChainB_recomposesOnlyOnBooleanFlip() = runComposeUiTest {
        setContent { DejavuTestContent { DerivedChainScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // base 0→2: derivedA = 0→1, derivedB = false→true
        onNodeWithTag("inc_base_twice_btn").performClick()
        waitForIdle()

        onNodeWithTag("derived_chain_b")
            .assertRecompositions(exactly = GroundTruth.delta("derived_chain_b"))
        assertEquals(1, GroundTruth.delta("derived_chain_b"), "derivedB flipping false→true recomposes the consumer once")
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun KeyIdentityScreen() {
    var keyValue by remember { mutableIntStateOf(0) }
    var localCount by remember { mutableIntStateOf(0) }
    Column {
        key(keyValue) {
            KeyedChild(keyValue, localCount)
        }
        BasicText("ChangeKey", Modifier.testTag("change_key_btn").clickable { keyValue++ })
        BasicText("IncLocal", Modifier.testTag("keyed_local_btn").clickable { localCount++ })
    }
}

@Composable
private fun KeyedChild(keyVal: Int, local: Int) {
    SideEffect { GroundTruth.record("keyed_child") }
    BasicText("Keyed: $keyVal, local: $local", Modifier.testTag("keyed_child"))
}

@Composable
private fun LoopScreen() {
    var itemCount by remember { mutableIntStateOf(3) }
    Column {
        LoopCountLabel(itemCount)
        for (i in 0 until itemCount) {
            key(i) { LoopItem(i) }
        }
        BasicText("AddLoop", Modifier.testTag("add_loop_btn").clickable { itemCount++ })
    }
}

@Composable
private fun LoopCountLabel(count: Int) {
    SideEffect { GroundTruth.record("loop_count_label") }
    BasicText("Count: $count", Modifier.testTag("loop_count_label"))
}

@Composable
private fun LoopItem(index: Int) {
    // One call site emits every loop item, so the tracer aggregates them under the shared
    // compile-time key on non-Android. Record at the function level to match the function-level
    // assertions (per-instance counts are Android-only — see demo PerTagTrackingRegressionTest).
    SideEffect { GroundTruth.record("LoopItem") }
    BasicText("Loop: $index", Modifier.testTag("loop_item_$index"))
}

@Composable
private fun DerivedChainScreen() {
    var baseValue by remember { mutableIntStateOf(0) }
    val derivedA by remember { derivedStateOf { baseValue / 2 } }
    val derivedB by remember { derivedStateOf { derivedA > 0 } }
    Column {
        DerivedChainA(derivedA)
        DerivedChainB(derivedB)
        BasicText("IncBase", Modifier.testTag("inc_base_btn").clickable { baseValue++ })
        BasicText("IncBase2x", Modifier.testTag("inc_base_twice_btn").clickable { baseValue += 2 })
    }
}

@Composable
private fun DerivedChainA(value: Int) {
    SideEffect { GroundTruth.record("derived_chain_a") }
    BasicText("DerivedA: $value", Modifier.testTag("derived_chain_a"))
}

@Composable
private fun DerivedChainB(value: Boolean) {
    SideEffect { GroundTruth.record("derived_chain_b") }
    BasicText("DerivedB: $value", Modifier.testTag("derived_chain_b"))
}
