package dejavu

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-platform port of Android ExpandableCardTest.
 * Validates accordion using Foundation-only composables (Column + AnimatedVisibility).
 *
 * Every recomposition assertion is exact and self-validating against a [GroundTruth] `SideEffect`
 * (the runtime runs the effect after every successful composition, so it is the real composition
 * count):
 * - **Single-instance** nodes — [AccordionTitle] (`accordion_title`) and the two `Column`s in
 *   [AccordionScreen] (`accordion_list_root`, `accordion_list`) — have unique composer keys, so the
 *   public per-tag API resolves their exact count on all platforms → `exactly = delta(tag)`.
 * - The five [ExpandableCard]s are emitted from a keyless `repeat(5)` loop, so they share one
 *   composer key and their per-*instance* counts only resolve on Android (Choreographer
 *   fingerprinting). On the common targets the public per-tag count falls back to the shared
 *   *function-level* sum, so these tests assert the **function-level** count —
 *   `DejavuTracer.getRecompositionCount("dejavu.ExpandableCard")` == `GroundTruth.delta("ExpandableCard")`
 *   (tracer == real total recompositions across all cards). Per-*instance* card isolation is
 *   covered on Android by the demo instrumented tests.
 *
 * Expand/collapse drives a finite `AnimatedVisibility`; each test `waitForIdle()`s to settle the
 * animation before asserting, then compares the tracer against the post-settle ground truth so the
 * (animation-dependent, non-hardcodable) total is still verified exactly rather than directionally.
 */
@OptIn(ExperimentalTestApi::class)
class ExpandableCardPatternTest {

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
    fun accordion_clicked_card_recomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        // Expanding card 0 flips its isExpanded false→true and runs the AnimatedVisibility reveal.
        // The cards come from a keyless loop, so per-instance counts are Android-only; on the common
        // targets the public per-tag count falls back to the function-level sum. Assert tracer ==
        // ground truth at the function level (settled), then that at least the clicked card composed.
        assertEquals(
            GroundTruth.delta("ExpandableCard"),
            DejavuTracer.getRecompositionCount("dejavu.ExpandableCard"),
            "tracer ExpandableCard count should equal SideEffect ground truth",
        )
        // KEEP directional: the exact total is the finite AnimatedVisibility reveal frame count,
        // which is animation-implementation-dependent and not safely hardcodable. The exact
        // recomposition assertion is the tracer == ground-truth check above; this is a behavioral
        // floor confirming the clicked card actually composed.
        assertTrue(
            GroundTruth.delta("ExpandableCard") >= 1,
            "expanding card 0 must recompose at least the clicked card",
        )
    }

    @Test
    fun accordion_previously_expanded_card_recomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        // Expand card 0
        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Expand card 1 (collapses card 0)
        onNodeWithTag("card_header_1").performClick()
        waitForIdle()

        // card 0 collapses (isExpanded true→false) and card 1 expands (false→true) — both recompose,
        // plus their AnimatedVisibility transitions. Function-level tracer == ground truth (settled),
        // and at least the two involved cards composed.
        assertEquals(
            GroundTruth.delta("ExpandableCard"),
            DejavuTracer.getRecompositionCount("dejavu.ExpandableCard"),
            "tracer ExpandableCard count should equal SideEffect ground truth",
        )
        // KEEP directional: the exact total is the finite AnimatedVisibility collapse+reveal frame
        // count, which is animation-implementation-dependent and not safely hardcodable. The exact
        // recomposition assertion is the tracer == ground-truth check above; this is a behavioral
        // floor confirming both involved cards composed.
        assertTrue(
            GroundTruth.delta("ExpandableCard") >= 2,
            "collapsing card 0 and expanding card 1 must recompose both involved cards",
        )
    }

    @Test
    fun accordion_uninvolved_cards_stable() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        // Expand card 0
        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Expand card 1 (collapses card 0)
        onNodeWithTag("card_header_1").performClick()
        waitForIdle()

        onNodeWithTag("card_2").assertIsDisplayed()
        onNodeWithTag("card_3").assertIsDisplayed()
        onNodeWithTag("card_4").assertIsDisplayed()

        // Cards 2,3,4 keep isExpanded == false across the card-0→card-1 toggle, so none of them
        // recomposes. The cards share one composer key on the common targets, so the only exact,
        // self-validating handle on stability is the function-level total: tracer == ground truth
        // (settled). Whatever the involved cards (0 collapsing, 1 expanding) and their finite
        // AnimatedVisibility transitions do, Dejavu must report it exactly.
        assertEquals(
            GroundTruth.delta("ExpandableCard"),
            DejavuTracer.getRecompositionCount("dejavu.ExpandableCard"),
            "tracer ExpandableCard count should equal SideEffect ground truth",
        )
    }

    @Test
    fun accordion_content_appears_on_expand() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        onNodeWithTag("card_content_0").assertIsDisplayed()
    }

    @Test
    fun accordion_content_disappears_on_collapse() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        // Expand card 0
        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        // Expand card 1 (collapses card 0)
        onNodeWithTag("card_header_1").performClick()
        waitForIdle()

        // AnimatedVisibility hides content; verify it's no longer displayed
        // Use kotlin.test assertion since assertDoesNotExist isn't in KMP compose test
        val nodes = onAllNodesWithTag("card_content_0").fetchSemanticsNodes()
        assertTrue(nodes.isEmpty() || nodes.all {
            try { it.boundsInRoot; false } catch (_: Throwable) { true }
        }, "card_content_0 should not be displayed after collapse")
    }

    @Test
    fun accordion_title_stays_stable() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        // The title is parameterless and outside the toggled state, so it never recomposes.
        onNodeWithTag("accordion_title")
            .assertRecompositions(exactly = GroundTruth.delta("accordion_title"))
        assertEquals(0, GroundTruth.delta("accordion_title"), "parameterless title never recomposes")
        onNodeWithTag("accordion_title").assertStable()
    }

    @Test
    fun accordion_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No interaction occurred, so every tracked node must be stable.
        onNodeWithTag("accordion_title").assertStable()
        onNodeWithTag("accordion_list_root").assertStable()
        onNodeWithTag("accordion_list").assertStable()

        // Single-instance Columns resolve their exact per-tag count on all platforms.
        onNodeWithTag("accordion_title")
            .assertRecompositions(exactly = GroundTruth.delta("accordion_title"))
        onNodeWithTag("accordion_list_root")
            .assertRecompositions(exactly = GroundTruth.delta("accordion_list_root"))
        onNodeWithTag("accordion_list")
            .assertRecompositions(exactly = GroundTruth.delta("accordion_list"))
        assertEquals(0, GroundTruth.delta("accordion_title"))
        assertEquals(0, GroundTruth.delta("accordion_list_root"))
        assertEquals(0, GroundTruth.delta("accordion_list"))

        // The five cards come from one call site, so assert the function-level total exactly:
        // with no interaction, no card composes after the baseline.
        assertEquals(
            GroundTruth.delta("ExpandableCard"),
            DejavuTracer.getRecompositionCount("dejavu.ExpandableCard"),
            "tracer ExpandableCard count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("ExpandableCard"), "no card composes without interaction")
    }

    @Test
    fun accordion_multiple_expansions_tracked() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        // Expand card 0
        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Expand card 1
        onNodeWithTag("card_header_1").performClick()
        waitForIdle()

        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // Expand card 2
        onNodeWithTag("card_header_2").performClick()
        waitForIdle()

        // Across the final reset boundary, expanding card 2 collapses card 1 and expands card 2 —
        // both recompose (plus their AnimatedVisibility transitions). Function-level tracer ==
        // ground truth (settled), and at least the two involved cards composed.
        assertEquals(
            GroundTruth.delta("ExpandableCard"),
            DejavuTracer.getRecompositionCount("dejavu.ExpandableCard"),
            "tracer ExpandableCard count should equal SideEffect ground truth",
        )
        // KEEP directional: the exact total is the finite AnimatedVisibility collapse+reveal frame
        // count, which is animation-implementation-dependent and not safely hardcodable. The exact
        // recomposition assertion is the tracer == ground-truth check above; this is a behavioral
        // floor confirming both involved cards composed.
        assertTrue(
            GroundTruth.delta("ExpandableCard") >= 2,
            "expanding card 2 collapses card 1 and expands card 2 — both involved cards recompose",
        )
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun AccordionScreen() {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    // Both Columns live in this scope and recompose exactly when AccordionScreen recomposes; record
    // each tag's ground truth so the single-instance per-tag assertions line up on all platforms.
    SideEffect { GroundTruth.record("accordion_list_root") }
    SideEffect { GroundTruth.record("accordion_list") }
    Column(Modifier.testTag("accordion_list_root")) {
        AccordionTitle()
        Column(Modifier.testTag("accordion_list")) {
            repeat(5) { index ->
                ExpandableCard(
                    index = index,
                    isExpanded = expandedIndex == index,
                    onToggle = { expandedIndex = if (expandedIndex == index) null else index }
                )
            }
        }
    }
}

@Composable
private fun AccordionTitle() {
    SideEffect { GroundTruth.record("accordion_title") }
    BasicText("Accordion", Modifier.testTag("accordion_title"))
}

@Composable
private fun ExpandableCard(index: Int, isExpanded: Boolean, onToggle: () -> Unit) {
    // One call site (repeat(5)) emits every card, so the tracer aggregates them under the shared
    // compile-time key on non-Android. Record at the function level to match the function-level
    // assertions (per-instance counts are Android-only — see demo instrumented tests).
    SideEffect { GroundTruth.record("ExpandableCard") }
    Column(Modifier.testTag("card_$index")) {
        BasicText("Card $index", Modifier.testTag("card_header_$index").clickable { onToggle() })
        AnimatedVisibility(isExpanded) {
            BasicText("Content of card $index", Modifier.testTag("card_content_$index"))
        }
    }
}
