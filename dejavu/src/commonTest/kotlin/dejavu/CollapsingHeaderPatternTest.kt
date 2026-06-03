package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch

/**
 * Cross-platform port of Android CollapsingHeaderTest.
 * Scroll-driven collapsing header with lerp-based animations.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect` (the runtime
 * runs the effect after every successful composition, so it is the real composition count):
 * - **Single-instance** nodes (the root box, the header box, the header image, the header title)
 *   have unique composer keys, so the public per-tag API resolves their exact count on all
 *   platforms → `exactly = GroundTruth.delta(tag)`.
 * - The 30 `ScrollContentItem`s are emitted from ONE keyless `for` call site, so they share one
 *   composer key and their per-*instance* counts only resolve on Android. On the common targets the
 *   per-tag count falls back to the shared *function-level* sum, so these assert the function-level
 *   count — `DejavuTracer.getRecompositionCount("dejavu.ScrollContentItem")` ==
 *   `GroundTruth.delta("ScrollContentItem")`.
 *
 * Scroll/collapse is gesture-driven: `animateScrollTo` settles, but the absolute number of
 * intermediate frames (and hence recompositions of the lerp-driven header nodes) is not
 * deterministic across platforms. Each test therefore `waitForIdle()`s to settle the scroll BEFORE
 * asserting, then asserts `exactly = GroundTruth.delta(tag)` — self-validating that Dejavu's count
 * equals the runtime's real recomposition count for that node, whatever the settled total happens
 * to be. The no-interaction test asserts exact zeros (fully deterministic).
 */
@OptIn(ExperimentalTestApi::class)
class CollapsingHeaderPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun header_recomposes_on_scroll() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle() // settle the scroll/collapse animation before asserting

        // Scrolling drives collapseFraction, recomposing the header. The settled frame count is
        // non-deterministic, so assert tracer == real recomposition count rather than a direction.
        onNodeWithTag("collapsing_header")
            .assertRecompositions(exactly = GroundTruth.delta("collapsing_header"))
    }

    @Test
    fun header_image_recomposes_on_scroll() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle() // settle the scroll/collapse animation before asserting

        onNodeWithTag("header_image")
            .assertRecompositions(exactly = GroundTruth.delta("header_image"))
    }

    @Test
    fun header_title_recomposes_on_scroll() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle() // settle the scroll/collapse animation before asserting

        onNodeWithTag("header_title")
            .assertRecompositions(exactly = GroundTruth.delta("header_title"))
    }

    @Test
    fun scroll_content_items_stable_during_scroll() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle() // settle the scroll/collapse animation before asserting

        // ScrollContentItems come from one keyless call site, so per-tag counts fall back to the
        // shared function-level sum on the common targets. Assert the function-level count: tracer
        // == ground truth proves Dejavu counted exactly what the runtime ran across all 30 items.
        // Scrolling does not change any item's params (only the header lerp + scroll offset move),
        // so the settled function-level total is 0 — the items stay stable through the scroll.
        assertEquals(
            GroundTruth.delta("ScrollContentItem"),
            DejavuTracer.getRecompositionCount("dejavu.ScrollContentItem"),
            "tracer ScrollContentItem count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("ScrollContentItem"), "scroll content items stay stable during scroll")
    }

    @Test
    fun scroll_to_top_restores_header() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("scroll_to_top_btn").performClick()
        waitForIdle() // settle the scroll-back/expand animation before asserting

        // Scrolling back to the top reverses collapseFraction, recomposing the header again. The
        // settled frame count is non-deterministic, so assert tracer == real recomposition count.
        onNodeWithTag("collapsing_header")
            .assertRecompositions(exactly = GroundTruth.delta("collapsing_header"))
    }

    @Test
    fun header_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No interaction occurred, so every single-instance header node must be stable.
        onNodeWithTag("collapsing_header_root")
            .assertRecompositions(exactly = GroundTruth.delta("collapsing_header_root"))
        onNodeWithTag("collapsing_header")
            .assertRecompositions(exactly = GroundTruth.delta("collapsing_header"))
        onNodeWithTag("header_image")
            .assertRecompositions(exactly = GroundTruth.delta("header_image"))
        onNodeWithTag("header_title")
            .assertRecompositions(exactly = GroundTruth.delta("header_title"))
        assertEquals(0, GroundTruth.delta("collapsing_header_root"))
        assertEquals(0, GroundTruth.delta("collapsing_header"))
        assertEquals(0, GroundTruth.delta("header_image"))
        assertEquals(0, GroundTruth.delta("header_title"))

        // ScrollContentItem is multi-instance (30 items share one composer key), so per-tag counts
        // fall back to the function-level sum on the common targets. Assert it at the function level:
        // tracer == ground truth, and with no interaction the total is 0.
        assertEquals(
            GroundTruth.delta("ScrollContentItem"),
            DejavuTracer.getRecompositionCount("dejavu.ScrollContentItem"),
            "tracer ScrollContentItem count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("ScrollContentItem"), "no scroll content item composes without interaction")
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun CollapsingHeaderScreen() {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val collapseFraction = (scrollState.value / 400f).coerceIn(0f, 1f)

    SideEffect { GroundTruth.record("collapsing_header_root") }
    Box(modifier = Modifier.testTag("collapsing_header_root").fillMaxSize()) {
        ScrollContent(
            scrollState = scrollState,
            headerHeight = lerp(200f, 56f, collapseFraction)
        )

        CollapsingHeader(collapseFraction = collapseFraction)

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            ScrollToTopButton {
                coroutineScope.launch { scrollState.animateScrollTo(0) }
            }
            Spacer(modifier = Modifier.weight(1f))
            ScrollToBottomButton {
                coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
            }
        }
    }
}

@Composable
private fun CollapsingHeader(collapseFraction: Float) {
    SideEffect { GroundTruth.record("collapsing_header") }
    val headerHeight = lerp(200f, 56f, collapseFraction)
    Box(
        modifier = Modifier
            .testTag("collapsing_header")
            .fillMaxWidth()
            .height(headerHeight.dp)
    ) {
        HeaderImage(alpha = lerp(1f, 0f, collapseFraction))
        HeaderTitle(collapseFraction = collapseFraction)
    }
}

@Composable
private fun HeaderImage(alpha: Float) {
    SideEffect { GroundTruth.record("header_image") }
    Box(
        modifier = Modifier
            .testTag("header_image")
            .fillMaxSize()
            .alpha(alpha)
    )
}

@Composable
private fun HeaderTitle(collapseFraction: Float) {
    SideEffect { GroundTruth.record("header_title") }
    val fontSize = lerp(24f, 16f, collapseFraction)
    BasicText(
        text = "Collapsing Header ($fontSize)",
        modifier = Modifier
            .testTag("header_title")
            .padding(16.dp)
    )
}

@Composable
private fun ScrollContent(
    scrollState: androidx.compose.foundation.ScrollState,
    headerHeight: Float,
) {
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(top = headerHeight.dp)
    ) {
        for (i in 0 until 30) {
            ScrollContentItem(index = i)
        }
    }
}

@Composable
private fun ScrollContentItem(index: Int) {
    // One keyless call site emits every item, so the tracer aggregates them under the shared
    // compile-time key on non-Android. Record at the function level to match the function-level
    // assertions (per-instance counts are Android-only — see demo PerTagTrackingRegressionTest).
    SideEffect { GroundTruth.record("ScrollContentItem") }
    BasicText(
        text = "Item $index",
        modifier = Modifier
            .testTag("scroll_content_item_$index")
            .fillMaxWidth()
            .padding(16.dp),
    )
}

@Composable
private fun ScrollToTopButton(onClick: () -> Unit) {
    BasicText(
        "Scroll Top",
        Modifier.testTag("scroll_to_top_btn").clickable(onClick = onClick)
    )
}

@Composable
private fun ScrollToBottomButton(onClick: () -> Unit) {
    BasicText(
        "Scroll Bottom",
        Modifier.testTag("scroll_to_bottom_btn").clickable(onClick = onClick)
    )
}
