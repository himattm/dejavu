package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-platform accuracy tests mirroring the demo app's composable pattern coverage.
 * Each test defines composables inline, uses [SideEffect] as ground truth, and
 * validates against [DejavuTracer.getRecompositionCount] by function name.
 *
 * Covers: counters, derived state, deep nesting, shared state, lazy lists,
 * CompositionLocal (static & dynamic), key identity, child isolation,
 * same-value writes, conditional composition, list mutations, and
 * LaunchedEffect restarts.
 */
@OptIn(ExperimentalTestApi::class)
class ComposablePatternAccuracyTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        groundTruth.clear()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    // ── Helpers ────────────────────────────────────────────────

    /** Asserts that Dejavu count matches SideEffect ground truth for a function. */
    private fun assertAccuracy(qualifiedName: String, label: String = qualifiedName) {
        val gt = groundTruth.recompositions(qualifiedName)
        val dv = DejavuTracer.getRecompositionCount(qualifiedName)
        assertEquals(gt, dv, "[$label] groundTruth=$gt, dejavu=$dv")
    }

    /**
     * Asserts per-tag recomposition count matches SideEffect ground truth.
     * Validates the full tag mapping pipeline: LocalInspectionTables → CompositionData →
     * CommonTagMapping → testTag → function name → per-tag count.
     */
    private fun assertTagAccuracy(tag: String, qualifiedName: String, label: String = tag) {
        refreshTagMapping()
        // Verify tag → function mapping exists
        val mappedFunction = DejavuTracer.getFunctionNameForTag(tag)
        assertTrue(mappedFunction != null,
            "[$label] tag '$tag' should be mapped to a function, but got null. " +
            "Known tags: ${DejavuTracer.testTagToFunction.keys}")
        // Resolve recomposition count using same logic as DejavuComposeTestRule
        val count = if (DejavuTracer.isMultiInstanceFunction(mappedFunction)) {
            DejavuTracer.getPerTagRecompositionCount(tag) ?: 0
        } else {
            DejavuTracer.getRecompositionCount(mappedFunction)
        }
        val gt = groundTruth.recompositions(qualifiedName)
        assertEquals(gt, count,
            "[$label] per-tag: groundTruth=$gt, dejavu=$count (function=$mappedFunction)")
    }

    // ── 1. Simple Counter (with per-tag tracking) ─────────────

    @Test
    fun counter_stateChanges_matchGroundTruth() = runComposeUiTest {
        setContent { DejavuTestContent { PatternCounter() } }
        waitForIdle()

        repeat(3) {
            onNodeWithTag("counter_inc").performClick()
            waitForIdle()
        }
        onNodeWithTag("counter_reset").performClick()
        waitForIdle()

        // Function-name accuracy
        assertAccuracy("dejavu.PatternCounter", "Counter")
        assertAccuracy("dejavu.CounterDisplay", "CounterDisplay")

        // Per-tag accuracy (validates tag mapping pipeline)
        // Debug: check if inspection tables were populated
        assertTrue(testInspectionTables.isNotEmpty(),
            "LocalInspectionTables should be populated by Compose runtime, but got ${testInspectionTables.size} entries")
        assertTagAccuracy("counter_display", "dejavu.CounterDisplay", "CounterDisplay-tag")
    }

    // ── 2. Derived State ──────────────────────────────────────

    @Test
    fun derivedState_onlyRecomposesWhenDerivedChanges() = runComposeUiTest {
        setContent { DejavuTestContent { PatternDerivedState() } }
        waitForIdle()

        // Click 3 times: count goes 0→1→2→3, isEven flips each time
        repeat(3) {
            onNodeWithTag("derived_inc").performClick()
            waitForIdle()
        }

        assertAccuracy("dejavu.PatternDerivedState", "DerivedState")
        assertAccuracy("dejavu.DerivedReader", "DerivedReader")

        // Per-tag accuracy
        assertTagAccuracy("derived_reader", "dejavu.DerivedReader", "DerivedReader-tag")
    }

    // ── 3. Deep Nesting ───────────────────────────────────────

    @Test
    fun deepNesting_allLevelsRecomposeOnSharedState() = runComposeUiTest {
        setContent { DejavuTestContent { PatternDeepNesting() } }
        waitForIdle()

        repeat(3) {
            onNodeWithTag("deep_inc").performClick()
            waitForIdle()
        }

        assertAccuracy("dejavu.PatternDeepNesting", "DeepRoot")
        assertAccuracy("dejavu.DeepLevel1", "Level1")
        assertAccuracy("dejavu.DeepLevel2", "Level2")
        assertAccuracy("dejavu.DeepLevel3", "Level3")

        // Per-tag accuracy on deep leaf
        assertTagAccuracy("deep_leaf", "dejavu.DeepLevel3", "DeepLeaf-tag")
    }

    // ── 4. Shared State / Multiple Readers ────────────────────

    @Test
    fun sharedState_allReadersRecomposeOnStateChange() = runComposeUiTest {
        setContent { DejavuTestContent { PatternSharedState() } }
        waitForIdle()

        onNodeWithTag("shared_inc_a").performClick()
        waitForIdle()
        onNodeWithTag("shared_inc_b").performClick()
        waitForIdle()

        assertAccuracy("dejavu.SharedReaderA", "ReaderA")
        assertAccuracy("dejavu.SharedReaderB", "ReaderB")
        assertAccuracy("dejavu.SharedDualReader", "DualReader")

        // Per-tag accuracy on shared readers
        assertTagAccuracy("shared_a", "dejavu.SharedReaderA", "SharedA-tag")
        assertTagAccuracy("shared_b", "dejavu.SharedReaderB", "SharedB-tag")
        assertTagAccuracy("shared_dual", "dejavu.SharedDualReader", "SharedDual-tag")
    }

    // ── 5. Lazy List ──────────────────────────────────────────

    @Test
    fun lazyColumn_headerAndFooterTrackRecompositions() = runComposeUiTest {
        setContent { DejavuTestContent { PatternLazyList() } }
        waitForIdle()

        onNodeWithTag("lazy_select").performClick()
        waitForIdle()
        onNodeWithTag("lazy_clear").performClick()
        waitForIdle()

        assertAccuracy("dejavu.LazyListHeader", "LazyHeader")
        assertAccuracy("dejavu.LazyListFooter", "LazyFooter")
    }

    // ── 6. LazyRow Variant ────────────────────────────────────

    @Test
    fun lazyRow_selectionTracksRecompositions() = runComposeUiTest {
        setContent { DejavuTestContent { PatternLazyRow() } }
        waitForIdle()

        onNodeWithTag("row_select").performClick()
        waitForIdle()
        onNodeWithTag("row_clear").performClick()
        waitForIdle()

        assertAccuracy("dejavu.LazyRowHeader", "LazyRowHeader")
    }

    // ── 7. Static CompositionLocal ────────────────────────────

    @Test
    fun staticCompositionLocal_recomposesAllReaders() = runComposeUiTest {
        setContent { DejavuTestContent { PatternStaticLocal() } }
        waitForIdle()

        onNodeWithTag("static_change").performClick()
        waitForIdle()
        onNodeWithTag("static_change").performClick()
        waitForIdle()

        assertAccuracy("dejavu.StaticLocalReaderA", "StaticReaderA")
        assertAccuracy("dejavu.StaticLocalReaderB", "StaticReaderB")

        // Per-tag accuracy
        assertTagAccuracy("static_a", "dejavu.StaticLocalReaderA", "StaticA-tag")
        assertTagAccuracy("static_b", "dejavu.StaticLocalReaderB", "StaticB-tag")
    }

    // ── 8. Dynamic CompositionLocal ───────────────────────────

    @Test
    fun dynamicCompositionLocal_recomposesOnlyDirectReaders() = runComposeUiTest {
        setContent { DejavuTestContent { PatternDynamicLocal() } }
        waitForIdle()

        onNodeWithTag("dynamic_change").performClick()
        waitForIdle()

        assertAccuracy("dejavu.DynamicLocalReader", "DynamicReader")
        // Unrelated composable should not recompose from CompositionLocal change
        val unrelatedGt = groundTruth.recompositions("dejavu.DynamicLocalUnrelated")
        val unrelatedDv = DejavuTracer.getRecompositionCount("dejavu.DynamicLocalUnrelated")
        assertEquals(unrelatedGt, unrelatedDv, "Unrelated should match ground truth")
        assertEquals(0, unrelatedDv, "Unrelated should have 0 recompositions from dynamic local change")
    }

    // ── 9. Key Identity ──────────────────────────────────────

    @Test
    fun keyBlock_changeKeyRecreatesChild() = runComposeUiTest {
        setContent { DejavuTestContent { PatternKeyIdentity() } }
        waitForIdle()

        onNodeWithTag("key_change").performClick()
        waitForIdle()

        assertAccuracy("dejavu.PatternKeyIdentity", "KeyRoot")
        // KeyedChild is recreated (new key), so Dejavu sees new initial composition
        // plus the old one had a recomposition from the parent state change
        assertAccuracy("dejavu.KeyedChild", "KeyedChild")
    }

    // ── 10. Child Isolation ──────────────────────────────────

    @Test
    fun stableChild_doesNotRecomposeWhenParentDoes() = runComposeUiTest {
        setContent { DejavuTestContent { PatternChildIsolation() } }
        waitForIdle()

        // Only parent state changes; child params are stable
        onNodeWithTag("isolation_inc").performClick()
        waitForIdle()

        assertAccuracy("dejavu.PatternChildIsolation", "Parent")
        assertAccuracy("dejavu.StableChild", "StableChild")
        assertEquals(0, DejavuTracer.getRecompositionCount("dejavu.StableChild"),
            "Stable child should not recompose when parent changes unrelated state")

        // Per-tag accuracy
        assertTagAccuracy("stable_child", "dejavu.StableChild", "StableChild-tag")
    }

    // ── 11. Same-Value Writes ────────────────────────────────

    @Test
    fun sameValueWrite_doesNotTriggerRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { PatternSameValueWrite() } }
        waitForIdle()

        // Write the same value — should NOT trigger recomposition
        onNodeWithTag("same_value_write").performClick()
        waitForIdle()
        onNodeWithTag("same_value_write").performClick()
        waitForIdle()

        assertAccuracy("dejavu.SameValueReader", "SameValueReader")
        assertEquals(0, DejavuTracer.getRecompositionCount("dejavu.SameValueReader"),
            "Same-value writes should not cause recomposition")
    }

    // ── 12. Conditional Composition ──────────────────────────

    @Test
    fun conditionalComposition_branchSwitchTracked() = runComposeUiTest {
        setContent { DejavuTestContent { PatternConditionalComposition() } }
        waitForIdle()

        // Toggle branch
        onNodeWithTag("cond_toggle").performClick()
        waitForIdle()
        // Toggle back
        onNodeWithTag("cond_toggle").performClick()
        waitForIdle()

        assertAccuracy("dejavu.PatternConditionalComposition", "ConditionalRoot")
    }

    // ── 13. List Mutations ───────────────────────────────────

    @Test
    fun listMutations_addRemoveTracked() = runComposeUiTest {
        setContent { DejavuTestContent { PatternListMutations() } }
        waitForIdle()

        onNodeWithTag("list_add").performClick()
        waitForIdle()
        onNodeWithTag("list_add").performClick()
        waitForIdle()
        onNodeWithTag("list_remove").performClick()
        waitForIdle()

        assertAccuracy("dejavu.ListMutationHeader", "ListHeader")
    }

    // ── 14. LaunchedEffect Restart ───────────────────────────

    @Test
    fun launchedEffectRestart_parentRecomposesOnKeyChange() = runComposeUiTest {
        setContent { DejavuTestContent { PatternLaunchedEffectRestart() } }
        waitForIdle()

        onNodeWithTag("effect_change_key").performClick()
        waitForIdle()
        onNodeWithTag("effect_change_key").performClick()
        waitForIdle()

        assertAccuracy("dejavu.PatternLaunchedEffectRestart", "EffectRestart")
    }

    // ── 15. Nested CompositionLocal Override ──────────────────

    @Test
    fun nestedCompositionLocal_innerOverrideTracked() = runComposeUiTest {
        setContent { DejavuTestContent { PatternNestedLocalOverride() } }
        waitForIdle()

        onNodeWithTag("nested_change_outer").performClick()
        waitForIdle()

        assertAccuracy("dejavu.OuterLocalReader", "OuterReader")
        assertAccuracy("dejavu.InnerLocalReader", "InnerReader")
    }

    // ── 16. Multi-Instance Per-Tag Tracking ─────────────────────

    @Test
    fun multiInstance_perTagCountsAreIndependent() = runComposeUiTest {
        setContent { DejavuTestContent { PatternMultiInstance() } }
        waitForIdle()
        refreshTagMapping() // Initial snapshot for fingerprint baseline

        // Only increment counter A — instance B should NOT recompose
        onNodeWithTag("multi_inc_a").performClick()
        waitForIdle()
        refreshTagMapping() // Snapshot after each action for per-instance detection

        onNodeWithTag("multi_inc_a").performClick()
        waitForIdle()
        refreshTagMapping()

        // Now increment counter B once
        onNodeWithTag("multi_inc_b").performClick()
        waitForIdle()
        refreshTagMapping()

        val tagA = "multi_display_a"
        val tagB = "multi_display_b"

        val mappedA = DejavuTracer.getFunctionNameForTag(tagA)
        val mappedB = DejavuTracer.getFunctionNameForTag(tagB)
        assertTrue(mappedA != null, "Tag '$tagA' should be mapped. Known: ${DejavuTracer.testTagToFunction.keys}")
        assertTrue(mappedB != null, "Tag '$tagB' should be mapped. Known: ${DejavuTracer.testTagToFunction.keys}")
        assertEquals("dejavu.MultiDisplay", mappedA, "Tag A should map to MultiDisplay")
        assertEquals("dejavu.MultiDisplay", mappedB, "Tag B should map to MultiDisplay")

        // Both tags map to the same function → isMultiInstanceFunction should be true
        assertTrue(DejavuTracer.isMultiInstanceFunction("dejavu.MultiDisplay"),
            "MultiDisplay should be detected as multi-instance")

        // Per-tag counts should be independent and match per-instance ground truth
        val countA = DejavuTracer.getPerTagRecompositionCount(tagA) ?: 0
        val countB = DejavuTracer.getPerTagRecompositionCount(tagB) ?: 0
        val gtA = groundTruth.recompositions("dejavu.MultiDisplay_a")
        val gtB = groundTruth.recompositions("dejavu.MultiDisplay_b")

        assertEquals(gtA, countA,
            "Instance A per-tag count ($countA) should match ground truth ($gtA)")
        assertEquals(gtB, countB,
            "Instance B per-tag count ($countB) should match ground truth ($gtB)")

        // Instance A should have MORE recompositions than instance B
        // (A was clicked 2x, B was clicked 1x)
        assertTrue(countA > countB,
            "Instance A ($countA recomps) should have more than B ($countB recomps)")
    }
}

// ══════════════════════════════════════════════════════════════
// Ground Truth Tracking
// ══════════════════════════════════════════════════════════════

private object groundTruth {
    private val counts = mutableMapOf<String, Int>()

    fun record(name: String) {
        counts[name] = (counts[name] ?: 0) + 1
    }

    /** Returns recomposition count (total SideEffect calls minus 1 for initial composition). */
    fun recompositions(name: String): Int = maxOf(0, (counts[name] ?: 0) - 1)

    fun clear() = counts.clear()
}

// ══════════════════════════════════════════════════════════════
// 1. Counter
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternCounter() {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { groundTruth.record("dejavu.PatternCounter") }
    Column {
        CounterDisplay(count)
        BasicText("Inc", Modifier.testTag("counter_inc").clickable { count++ })
        BasicText("Reset", Modifier.testTag("counter_reset").clickable { count = 0 })
    }
}

@Composable
private fun CounterDisplay(value: Int) {
    SideEffect { groundTruth.record("dejavu.CounterDisplay") }
    BasicText("Count: $value", Modifier.testTag("counter_display"))
}

// ══════════════════════════════════════════════════════════════
// 2. Derived State
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternDerivedState() {
    var count by remember { mutableIntStateOf(0) }
    val isEven by remember { derivedStateOf { count % 2 == 0 } }
    SideEffect { groundTruth.record("dejavu.PatternDerivedState") }
    Column {
        DerivedReader(isEven)
        BasicText("Inc", Modifier.testTag("derived_inc").clickable { count++ })
    }
}

@Composable
private fun DerivedReader(isEven: Boolean) {
    SideEffect { groundTruth.record("dejavu.DerivedReader") }
    BasicText("Even: $isEven", Modifier.testTag("derived_reader"))
}

// ══════════════════════════════════════════════════════════════
// 3. Deep Nesting
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternDeepNesting() {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { groundTruth.record("dejavu.PatternDeepNesting") }
    Column {
        DeepLevel1(count)
        BasicText("Inc", Modifier.testTag("deep_inc").clickable { count++ })
    }
}

@Composable
private fun DeepLevel1(value: Int) {
    SideEffect { groundTruth.record("dejavu.DeepLevel1") }
    Column { DeepLevel2(value) }
}

@Composable
private fun DeepLevel2(value: Int) {
    SideEffect { groundTruth.record("dejavu.DeepLevel2") }
    Column { DeepLevel3(value) }
}

@Composable
private fun DeepLevel3(value: Int) {
    SideEffect { groundTruth.record("dejavu.DeepLevel3") }
    BasicText("Deep: $value", Modifier.testTag("deep_leaf"))
}

// ══════════════════════════════════════════════════════════════
// 4. Shared State / Multiple Readers
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternSharedState() {
    var stateA by remember { mutableIntStateOf(0) }
    var stateB by remember { mutableIntStateOf(0) }
    Column {
        SharedReaderA(stateA)
        SharedReaderB(stateB)
        SharedDualReader(stateA, stateB)
        BasicText("IncA", Modifier.testTag("shared_inc_a").clickable { stateA++ })
        BasicText("IncB", Modifier.testTag("shared_inc_b").clickable { stateB++ })
    }
}

@Composable
private fun SharedReaderA(value: Int) {
    SideEffect { groundTruth.record("dejavu.SharedReaderA") }
    BasicText("A: $value", Modifier.testTag("shared_a"))
}

@Composable
private fun SharedReaderB(value: Int) {
    SideEffect { groundTruth.record("dejavu.SharedReaderB") }
    BasicText("B: $value", Modifier.testTag("shared_b"))
}

@Composable
private fun SharedDualReader(a: Int, b: Int) {
    SideEffect { groundTruth.record("dejavu.SharedDualReader") }
    BasicText("A+B: ${a + b}", Modifier.testTag("shared_dual"))
}

// ══════════════════════════════════════════════════════════════
// 5. Lazy List
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternLazyList() {
    var selected by remember { mutableStateOf<Int?>(null) }
    Column {
        LazyListHeader(selected)
        LazyColumn(Modifier.testTag("lazy_column")) {
            items(5) { index ->
                BasicText("Item $index", Modifier.testTag("lazy_item_$index"))
            }
        }
        LazyListFooter(selected)
        BasicText("Select", Modifier.testTag("lazy_select").clickable { selected = 0 })
        BasicText("Clear", Modifier.testTag("lazy_clear").clickable { selected = null })
    }
}

@Composable
private fun LazyListHeader(selected: Int?) {
    SideEffect { groundTruth.record("dejavu.LazyListHeader") }
    BasicText("Selected: ${selected ?: "none"}", Modifier.testTag("lazy_header"))
}

@Composable
private fun LazyListFooter(selected: Int?) {
    SideEffect { groundTruth.record("dejavu.LazyListFooter") }
    BasicText("Has selection: ${selected != null}", Modifier.testTag("lazy_footer"))
}

// ══════════════════════════════════════════════════════════════
// 6. LazyRow Variant
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternLazyRow() {
    var selected by remember { mutableStateOf<Int?>(null) }
    Column {
        LazyRowHeader(selected)
        LazyRow(Modifier.testTag("lazy_row")) {
            items(5) { index ->
                BasicText("Row $index", Modifier.testTag("row_item_$index"))
            }
        }
        BasicText("Select", Modifier.testTag("row_select").clickable { selected = 0 })
        BasicText("Clear", Modifier.testTag("row_clear").clickable { selected = null })
    }
}

@Composable
private fun LazyRowHeader(selected: Int?) {
    SideEffect { groundTruth.record("dejavu.LazyRowHeader") }
    BasicText("Row selected: ${selected ?: "none"}", Modifier.testTag("row_header"))
}

// ══════════════════════════════════════════════════════════════
// 7. Static CompositionLocal
// ══════════════════════════════════════════════════════════════

private val LocalStaticValue = staticCompositionLocalOf { 0 }

@Composable
private fun PatternStaticLocal() {
    var value by remember { mutableIntStateOf(0) }
    CompositionLocalProvider(LocalStaticValue provides value) {
        Column {
            StaticLocalReaderA()
            StaticLocalReaderB()
            BasicText("Change", Modifier.testTag("static_change").clickable { value++ })
        }
    }
}

@Composable
private fun StaticLocalReaderA() {
    val value = LocalStaticValue.current
    SideEffect { groundTruth.record("dejavu.StaticLocalReaderA") }
    BasicText("Static A: $value", Modifier.testTag("static_a"))
}

@Composable
private fun StaticLocalReaderB() {
    val value = LocalStaticValue.current
    SideEffect { groundTruth.record("dejavu.StaticLocalReaderB") }
    BasicText("Static B: $value", Modifier.testTag("static_b"))
}

// ══════════════════════════════════════════════════════════════
// 8. Dynamic CompositionLocal
// ══════════════════════════════════════════════════════════════

private val LocalDynamicValue = compositionLocalOf { 0 }

@Composable
private fun PatternDynamicLocal() {
    var value by remember { mutableIntStateOf(0) }
    CompositionLocalProvider(LocalDynamicValue provides value) {
        Column {
            DynamicLocalReader()
            DynamicLocalUnrelated()
            BasicText("Change", Modifier.testTag("dynamic_change").clickable { value++ })
        }
    }
}

@Composable
private fun DynamicLocalReader() {
    val value = LocalDynamicValue.current
    SideEffect { groundTruth.record("dejavu.DynamicLocalReader") }
    BasicText("Dynamic: $value", Modifier.testTag("dynamic_reader"))
}

@Composable
private fun DynamicLocalUnrelated() {
    SideEffect { groundTruth.record("dejavu.DynamicLocalUnrelated") }
    BasicText("Unrelated", Modifier.testTag("dynamic_unrelated"))
}

// ══════════════════════════════════════════════════════════════
// 9. Key Identity
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternKeyIdentity() {
    var keyValue by remember { mutableIntStateOf(0) }
    SideEffect { groundTruth.record("dejavu.PatternKeyIdentity") }
    Column {
        key(keyValue) {
            KeyedChild(keyValue)
        }
        BasicText("ChangeKey", Modifier.testTag("key_change").clickable { keyValue++ })
    }
}

@Composable
private fun KeyedChild(keyValue: Int) {
    SideEffect { groundTruth.record("dejavu.KeyedChild") }
    BasicText("Keyed: $keyValue", Modifier.testTag("keyed_child"))
}

// ══════════════════════════════════════════════════════════════
// 10. Child Isolation
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternChildIsolation() {
    var parentCount by remember { mutableIntStateOf(0) }
    SideEffect { groundTruth.record("dejavu.PatternChildIsolation") }
    Column {
        BasicText("Parent: $parentCount")
        StableChild("fixed")
        BasicText("Inc", Modifier.testTag("isolation_inc").clickable { parentCount++ })
    }
}

@Composable
private fun StableChild(label: String) {
    SideEffect { groundTruth.record("dejavu.StableChild") }
    BasicText("Child: $label", Modifier.testTag("stable_child"))
}

// ══════════════════════════════════════════════════════════════
// 11. Same-Value Writes
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternSameValueWrite() {
    var value by remember { mutableIntStateOf(42) }
    Column {
        SameValueReader(value)
        BasicText("WriteSame", Modifier.testTag("same_value_write").clickable { value = 42 })
    }
}

@Composable
private fun SameValueReader(value: Int) {
    SideEffect { groundTruth.record("dejavu.SameValueReader") }
    BasicText("Value: $value", Modifier.testTag("same_reader"))
}

// ══════════════════════════════════════════════════════════════
// 12. Conditional Composition
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternConditionalComposition() {
    var showA by remember { mutableStateOf(true) }
    SideEffect { groundTruth.record("dejavu.PatternConditionalComposition") }
    Column {
        if (showA) {
            BasicText("Branch A", Modifier.testTag("branch_a"))
        } else {
            BasicText("Branch B", Modifier.testTag("branch_b"))
        }
        BasicText("Toggle", Modifier.testTag("cond_toggle").clickable { showA = !showA })
    }
}

// ══════════════════════════════════════════════════════════════
// 13. List Mutations
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternListMutations() {
    val items = remember { mutableStateListOf("A", "B", "C") }
    Column {
        ListMutationHeader(items.size)
        LazyColumn(Modifier.testTag("mutation_list")) {
            items(items) { item ->
                BasicText(item, Modifier.testTag("mutation_item_$item"))
            }
        }
        BasicText("Add", Modifier.testTag("list_add").clickable { items.add("New${items.size}") })
        BasicText("Remove", Modifier.testTag("list_remove").clickable { if (items.isNotEmpty()) items.removeAt(items.lastIndex) })
    }
}

@Composable
private fun ListMutationHeader(count: Int) {
    SideEffect { groundTruth.record("dejavu.ListMutationHeader") }
    BasicText("Items: $count", Modifier.testTag("mutation_header"))
}

// ══════════════════════════════════════════════════════════════
// 14. LaunchedEffect Restart
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternLaunchedEffectRestart() {
    var effectKey by remember { mutableIntStateOf(0) }
    SideEffect { groundTruth.record("dejavu.PatternLaunchedEffectRestart") }
    LaunchedEffect(effectKey) {
        // Effect restarts when key changes — no-op body
    }
    Column {
        BasicText("Key: $effectKey", Modifier.testTag("effect_key_display"))
        BasicText("ChangeKey", Modifier.testTag("effect_change_key").clickable { effectKey++ })
    }
}

// ══════════════════════════════════════════════════════════════
// 15. Nested CompositionLocal Override
// ══════════════════════════════════════════════════════════════

private val LocalNested = compositionLocalOf { "outer" }

@Composable
private fun PatternNestedLocalOverride() {
    var outerValue by remember { mutableIntStateOf(0) }
    CompositionLocalProvider(LocalNested provides "outer-$outerValue") {
        Column {
            OuterLocalReader()
            CompositionLocalProvider(LocalNested provides "inner-$outerValue") {
                InnerLocalReader()
            }
            BasicText("ChangeOuter", Modifier.testTag("nested_change_outer").clickable { outerValue++ })
        }
    }
}

@Composable
private fun OuterLocalReader() {
    val value = LocalNested.current
    SideEffect { groundTruth.record("dejavu.OuterLocalReader") }
    BasicText("Outer: $value", Modifier.testTag("outer_reader"))
}

@Composable
private fun InnerLocalReader() {
    val value = LocalNested.current
    SideEffect { groundTruth.record("dejavu.InnerLocalReader") }
    BasicText("Inner: $value", Modifier.testTag("inner_reader"))
}

// ══════════════════════════════════════════════════════════════
// 16. Multi-Instance Per-Tag Tracking
// ══════════════════════════════════════════════════════════════

@Composable
private fun PatternMultiInstance() {
    var counterA by remember { mutableIntStateOf(0) }
    var counterB by remember { mutableIntStateOf(0) }
    Column {
        // Two instances of the SAME composable with different testTags and independent state
        MultiDisplay(counterA, Modifier.testTag("multi_display_a"), "a")
        MultiDisplay(counterB, Modifier.testTag("multi_display_b"), "b")
        BasicText("Inc A", Modifier.testTag("multi_inc_a").clickable { counterA++ })
        BasicText("Inc B", Modifier.testTag("multi_inc_b").clickable { counterB++ })
    }
}

@Composable
private fun MultiDisplay(value: Int, modifier: Modifier = Modifier, instanceId: String = "") {
    SideEffect { groundTruth.record("dejavu.MultiDisplay_$instanceId") }
    BasicText("Value: $value", modifier)
}
