package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
 * Cross-platform port of Android LazyListStressTest.
 * Validates LazyColumn with item selection, mutations, and resetCounts.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect` (the runtime
 * runs the effect after every successful composition, so it is the real composition count):
 * - **Single-instance** nodes (`list_header`, `selected_banner`, `derived_banner`) have unique
 *   composer keys, so the public per-tag API resolves their exact count on all platforms →
 *   `exactly = GroundTruth.delta(tag)`.
 * - The `ListItem`s are emitted from the `LazyColumn { items(5) { ... } }` loop, so they share one
 *   composer key and their per-*instance* counts only resolve on Android (Choreographer
 *   fingerprinting). On the common targets the public per-tag count falls back to the shared
 *   *function-level* sum, so these assert the **function-level** count —
 *   `DejavuTracer.getRecompositionCount("dejavu.ListItem")` == `GroundTruth.delta("ListItem")`
 *   (tracer == real total recompositions across all visible items). Per-instance isolation is
 *   covered on Android by the demo instrumented tests.
 *
 * Lazy virtualization makes the absolute item count platform-dependent (only laid-out items
 * compose), so the item-level tests assert `tracer == delta` rather than a hardcoded literal.
 *
 * Every test calls [resetRecompositionCounts] + [GroundTruth.snapshotBaseline] after the initial
 * `waitForIdle()` (and again after any mid-test reset). The reset zeroes the loop's
 * initial-composition artifact (the 5 items share one composer key); snapshotting the ground truth
 * at the same point keeps `delta`/tracer aligned. Scrolling/selection settles under `waitForIdle()`
 * before asserting.
 */
@OptIn(ExperimentalTestApi::class)
class LazyListStressPatternTest {

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
    fun header_neverRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        // The header is parameterless, so selecting an item must never recompose it.
        onNodeWithTag("list_header")
            .assertRecompositions(exactly = GroundTruth.delta("list_header"))
        assertEquals(0, GroundTruth.delta("list_header"), "parameterless header never recomposes")
    }

    @Test
    fun selectOneItem_bannerRecomposesOnce() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        // selected.size 0 → 1 changes the banner's param, recomposing it once.
        onNodeWithTag("selected_banner")
            .assertRecompositions(exactly = GroundTruth.delta("selected_banner"))
        assertEquals(1, GroundTruth.delta("selected_banner"), "banner recomposes once when selection count changes")
    }

    @Test
    fun selectOneItem_derivedBannerRecomposesOnce() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        // hasAnySelected false → true flips the derived banner's param, recomposing it once.
        onNodeWithTag("derived_banner")
            .assertRecompositions(exactly = GroundTruth.delta("derived_banner"))
        assertEquals(1, GroundTruth.delta("derived_banner"), "derived banner recomposes once on false→true flip")
    }

    @Test
    fun selectSecondItem_derivedBannerStaysStable() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        // Select first item (hasAnySelected goes from false to true)
        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Select all (hasAnySelected stays true — derived value unchanged)
        onNodeWithTag("select_all_btn").performClick()
        waitForIdle()

        // The derived value did not change, so the derived banner must not recompose.
        onNodeWithTag("derived_banner")
            .assertRecompositions(exactly = GroundTruth.delta("derived_banner"))
        assertEquals(0, GroundTruth.delta("derived_banner"), "unchanged derived value must not recompose the banner")
    }

    @Test
    fun selectAll_listItemRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("select_all_btn").performClick()
        waitForIdle()

        // Selecting all flips isSelected on every laid-out item. Lazy virtualization means only
        // the visible items compose, so assert the tracer's function-level ListItem count equals
        // the real recomposition total (tracer == ground truth) rather than a platform-dependent
        // literal.
        assertEquals(
            GroundTruth.delta("ListItem"),
            DejavuTracer.getRecompositionCount("dejavu.ListItem"),
            "tracer ListItem count should equal SideEffect ground truth",
        )
    }

    @Test
    fun clearAfterSelectAll_listItemRecomposesAgain() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        onNodeWithTag("select_all_btn").performClick()
        waitForIdle()

        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("clear_btn").performClick()
        waitForIdle()

        // Clearing flips isSelected back on every laid-out item. Tracer's function-level count
        // must equal the real recomposition total across the (virtualized) visible items.
        assertEquals(
            GroundTruth.delta("ListItem"),
            DejavuTracer.getRecompositionCount("dejavu.ListItem"),
            "tracer ListItem count should equal SideEffect ground truth",
        )
    }

    @Test
    fun tagMapping_lazyItems_resolveCorrectly() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        // Selecting item 0 recomposes only item 0's row. The loop items aggregate at the function
        // level on the common targets, so assert tracer == ground truth: whatever recomposed, the
        // tracer must report exactly that — proving lazy-item tag mapping does not over- or
        // under-count.
        assertEquals(
            GroundTruth.delta("ListItem"),
            DejavuTracer.getRecompositionCount("dejavu.ListItem"),
            "tracer ListItem count should equal SideEffect ground truth",
        )
    }

    @Test
    fun selectOneItem_listItemRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        // Selecting item 0 flips its isSelected param. Function-level: tracer == ground truth for
        // the ListItem call site (per-instance isolation is Android-only). The selected item must
        // recompose, so the count is the deterministic 1.
        assertEquals(
            GroundTruth.delta("ListItem"),
            DejavuTracer.getRecompositionCount("dejavu.ListItem"),
            "tracer ListItem count should equal SideEffect ground truth",
        )
        assertEquals(1, GroundTruth.delta("ListItem"), "selecting item 0 recomposes exactly that one row")
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun LazyListStressScreen() {
    val selected = remember { mutableStateListOf<Int>() }
    val hasAnySelected by remember { derivedStateOf { selected.isNotEmpty() } }
    Column {
        ListHeader()
        SelectedBanner(selected.size)
        DerivedBanner(hasAnySelected)
        LazyColumn {
            items(5) { index ->
                ListItem(index, index in selected)
            }
        }
        BasicText("Select0", Modifier.testTag("select_0_btn").clickable {
            if (0 !in selected) selected.add(0)
        })
        BasicText("SelectAll", Modifier.testTag("select_all_btn").clickable {
            selected.clear(); selected.addAll(0..4)
        })
        BasicText("Clear", Modifier.testTag("clear_btn").clickable { selected.clear() })
    }
}

@Composable
private fun ListHeader() {
    SideEffect { GroundTruth.record("list_header") }
    BasicText("Header", Modifier.testTag("list_header"))
}

@Composable
private fun SelectedBanner(count: Int) {
    SideEffect { GroundTruth.record("selected_banner") }
    BasicText("Selected: $count", Modifier.testTag("selected_banner"))
}

@Composable
private fun DerivedBanner(hasAny: Boolean) {
    SideEffect { GroundTruth.record("derived_banner") }
    BasicText("HasAny: $hasAny", Modifier.testTag("derived_banner"))
}

@Composable
private fun ListItem(index: Int, isSelected: Boolean) {
    // One call site (LazyColumn items {}) emits every row, so the tracer aggregates them under the
    // shared compile-time key on non-Android. Record at the function level to match the
    // function-level assertions (per-instance counts are Android-only — see demo instrumented tests).
    SideEffect { GroundTruth.record("ListItem") }
    BasicText("Item $index sel=$isSelected", Modifier.testTag("item_$index"))
}
