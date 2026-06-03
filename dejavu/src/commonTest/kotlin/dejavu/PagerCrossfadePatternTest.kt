package dejavu

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch

/**
 * Cross-platform port of Android PagerCrossfadeTest.
 * Validates HorizontalPager page tracking and Crossfade animation transitions.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect`: the Compose
 * runtime runs the effect after every successful composition, so the per-key `SideEffect` tally is
 * the real composition count. Pager flings and Crossfade transitions are finite — they settle — so
 * each test drives the clock to completion (`mainClock.advanceTimeBy(...)` + `waitForIdle()`) BEFORE
 * asserting, then compares Dejavu's count to the settled ground truth.
 *
 * Instance classification used below:
 * - **Single-instance** nodes (`pager_indicator`, `pager_sibling`, the three `Crossfade` variants,
 *   `crossfade_label`) each come from one call site with one testTag, so the public per-tag API
 *   resolves their exact count on all platforms → `exactly = GroundTruth.delta(tag)`.
 * - The pager pages are emitted from a single loop call site (`HorizontalPager { page -> }`), so the
 *   tracer aggregates them under the shared compile-time key on non-Android; those assert the
 *   **function-level** count — `DejavuTracer.getRecompositionCount("dejavu.PageContent")` ==
 *   `GroundTruth.delta("PageContent")`. Per-instance page isolation is Android-only (demo tests).
 *
 * First-composition subtlety (Crossfade variant B): Dejavu counts a key's *first* composition as the
 * initial composition, not a recomposition (`totalCount > 1`). A node that only appears *after* the
 * baseline therefore reports `0` recompositions even though the runtime composed it once. The
 * "variant B appears" test asserts exactly that: Dejavu == 0, while the ground-truth `SideEffect`
 * delta == 1 proves the node really did appear.
 */
@OptIn(ExperimentalTestApi::class)
class PagerCrossfadePatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    // --- HorizontalPager tests ---

    @Test
    fun pager_initialPageTracked() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()
        refreshTagMapping()

        // Page content is loop-emitted by HorizontalPager, so the tracer aggregates pages under the
        // shared compile-time key on non-Android → assert the function-level count. No interaction
        // occurred, so every initial page composition is pre-baseline and nothing recomposes after.
        assertEquals(
            GroundTruth.delta("PageContent"),
            DejavuTracer.getRecompositionCount("dejavu.PageContent"),
            "tracer PageContent count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("PageContent"), "no page composes after the baseline without interaction")
    }

    @Test
    fun pager_navigateToNextPage_indicatorRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("next_page_btn").performClick()
        waitForIdle()
        // Drive the finite fling to completion so currentPage settles before asserting.
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        refreshTagMapping()

        // pager_indicator is single-instance single-tag → exact per-tag count on all platforms.
        // animateScrollToPage(1) settles currentPage 0 → 1, a single param change.
        onNodeWithTag("pager_indicator")
            .assertRecompositions(exactly = GroundTruth.delta("pager_indicator"))
        assertEquals(1, GroundTruth.delta("pager_indicator"), "indicator recomposes once as currentPage settles 0→1")
    }

    @Test
    fun pager_navigateToNextPage_siblingStable() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("next_page_btn").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        refreshTagMapping()

        // The parameterless sibling is unaffected by the pager fling.
        onNodeWithTag("pager_sibling").assertStable()
        assertEquals(0, GroundTruth.delta("pager_sibling"), "parameterless sibling never recomposes during a fling")
    }

    // --- Crossfade tests ---

    @Test
    fun crossfade_initialVariantTracked() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()
        refreshTagMapping()

        // Variant A composes once at init (pre-baseline); no interaction → no recomposition after.
        onNodeWithTag("crossfade_a")
            .assertRecompositions(exactly = GroundTruth.delta("crossfade_a"))
        assertEquals(0, GroundTruth.delta("crossfade_a"), "initial variant does not recompose without interaction")
    }

    @Test
    fun crossfade_cycleToVariantB_labelRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("cycle_crossfade_btn").performClick()
        waitForIdle()
        // Let the finite crossfade transition settle before asserting.
        mainClock.advanceTimeBy(500)
        waitForIdle()
        refreshTagMapping()

        // crossfade_label is single-instance single-tag. crossfadeTarget 0→1 is one param change.
        onNodeWithTag("crossfade_label")
            .assertRecompositions(exactly = GroundTruth.delta("crossfade_label"))
        assertEquals(1, GroundTruth.delta("crossfade_label"), "label recomposes once as crossfade target goes 0→1")
    }

    @Test
    fun crossfade_cycleToVariantB_variantBAppears() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("cycle_crossfade_btn").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        refreshTagMapping()

        // Variant B is composed for the FIRST time only after the baseline. Dejavu counts a key's
        // first composition as the initial composition, not a recomposition, so it reports 0 — while
        // the ground-truth SideEffect delta is 1, proving the node actually appeared.
        onNodeWithTag("crossfade_b").assertRecompositions(exactly = 0)
        assertEquals(1, GroundTruth.delta("crossfade_b"), "variant B appears: it composes exactly once (its initial composition)")
    }

    @Test
    fun crossfade_siblingStable() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("cycle_crossfade_btn").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        refreshTagMapping()

        // The parameterless sibling is unaffected by the crossfade transition.
        onNodeWithTag("pager_sibling").assertStable()
        assertEquals(0, GroundTruth.delta("pager_sibling"), "parameterless sibling never recomposes during a crossfade")
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun PagerCrossfadeScreen() {
    val pagerState = rememberPagerState(pageCount = { 5 })
    var crossfadeTarget by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) { page ->
            PageContent(page)
        }

        PagerIndicator(pagerState.currentPage)

        Crossfade(targetState = crossfadeTarget, label = "crossfade") { target ->
            when (target) {
                0 -> CrossfadeVariantA()
                1 -> CrossfadeVariantB()
                else -> CrossfadeVariantC()
            }
        }

        CrossfadeLabel(crossfadeTarget)
        StaticPagerSibling()

        NextPageButton {
            coroutineScope.launch {
                val next = (pagerState.currentPage + 1).coerceAtMost(4)
                pagerState.animateScrollToPage(next)
            }
        }
        CycleCrossfadeButton { crossfadeTarget = (crossfadeTarget + 1) % 3 }
        ResetCrossfadeButton { crossfadeTarget = 0 }
    }
}

@Composable
private fun PageContent(page: Int) {
    // Pages are emitted from one HorizontalPager loop call site, so the tracer aggregates them under
    // the shared compile-time key on non-Android. Record at the function level to match the
    // function-level assertions (per-instance page counts are Android-only — see demo tests).
    SideEffect { GroundTruth.record("PageContent") }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .testTag("page_content_$page")
    ) {
        BasicText("Page $page")
    }
}

@Composable
private fun PagerIndicator(currentPage: Int) {
    SideEffect { GroundTruth.record("pager_indicator") }
    BasicText("Page: $currentPage", Modifier.testTag("pager_indicator"))
}

@Composable
private fun CrossfadeVariantA() {
    SideEffect { GroundTruth.record("crossfade_a") }
    BasicText("Variant A", Modifier.testTag("crossfade_a"))
}

@Composable
private fun CrossfadeVariantB() {
    SideEffect { GroundTruth.record("crossfade_b") }
    BasicText("Variant B", Modifier.testTag("crossfade_b"))
}

@Composable
private fun CrossfadeVariantC() {
    SideEffect { GroundTruth.record("crossfade_c") }
    BasicText("Variant C", Modifier.testTag("crossfade_c"))
}

@Composable
private fun CrossfadeLabel(target: Int) {
    SideEffect { GroundTruth.record("crossfade_label") }
    BasicText("Crossfade: $target", Modifier.testTag("crossfade_label"))
}

@Composable
private fun StaticPagerSibling() {
    SideEffect { GroundTruth.record("pager_sibling") }
    BasicText("Static sibling", Modifier.testTag("pager_sibling"))
}

@Composable
private fun NextPageButton(onClick: () -> Unit) {
    BasicText("Next Page", Modifier.testTag("next_page_btn").clickable { onClick() })
}

@Composable
private fun CycleCrossfadeButton(onClick: () -> Unit) {
    BasicText("Cycle", Modifier.testTag("cycle_crossfade_btn").clickable { onClick() })
}

@Composable
private fun ResetCrossfadeButton(onClick: () -> Unit) {
    BasicText("Reset", Modifier.testTag("reset_crossfade_btn").clickable { onClick() })
}
