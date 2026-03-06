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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.launch

/**
 * Cross-platform port of Android CollapsingHeaderTest.
 * Scroll-driven collapsing header with lerp-based animations.
 */
@OptIn(ExperimentalTestApi::class)
class CollapsingHeaderPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun header_recomposes_on_scroll() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle()

        onNodeWithTag("collapsing_header").assertRecompositions(atLeast = 1)
    }

    @Test
    fun header_image_recomposes_on_scroll() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle()

        onNodeWithTag("header_image").assertRecompositions(atLeast = 1)
    }

    @Test
    fun header_title_recomposes_on_scroll() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle()

        onNodeWithTag("header_title").assertRecompositions(atLeast = 1)
    }

    @Test
    fun scroll_content_items_stable_during_scroll() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle()

        onNodeWithTag("scroll_content_item_0").assertStable()
    }

    @Test
    fun scroll_to_top_restores_header() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()

        onNodeWithTag("scroll_to_bottom_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("scroll_to_top_btn").performClick()
        waitForIdle()

        onNodeWithTag("collapsing_header").assertRecompositions(atLeast = 1)
    }

    @Test
    fun header_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { CollapsingHeaderScreen() } }
        waitForIdle()

        onNodeWithTag("collapsing_header_root").assertStable()
        onNodeWithTag("collapsing_header").assertStable()
        onNodeWithTag("header_image").assertStable()
        onNodeWithTag("header_title").assertStable()
        // ScrollContentItem is multi-instance (30 items share qualified-name counter);
        // per-instance tracking not available here. Tested individually in
        // scroll_content_items_stable_during_scroll with resetRecompositionCounts.
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun CollapsingHeaderScreen() {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val collapseFraction = (scrollState.value / 400f).coerceIn(0f, 1f)

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
    Box(
        modifier = Modifier
            .testTag("header_image")
            .fillMaxSize()
            .alpha(alpha)
    )
}

@Composable
private fun HeaderTitle(collapseFraction: Float) {
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
