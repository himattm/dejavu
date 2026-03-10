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
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.launch

/**
 * Cross-platform port of Android PagerCrossfadeTest.
 * Validates HorizontalPager page tracking and Crossfade animation transitions.
 */
@OptIn(ExperimentalTestApi::class)
class PagerCrossfadePatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    // --- HorizontalPager tests ---

    @Test
    fun pager_initialPageTracked() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("page_content_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun pager_navigateToNextPage_indicatorRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()

        onNodeWithTag("next_page_btn").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("pager_indicator").assertRecompositions(atLeast = 1)
    }

    @Test
    fun pager_navigateToNextPage_siblingStable() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()

        onNodeWithTag("next_page_btn").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("pager_sibling").assertStable()
    }

    // --- Crossfade tests ---

    @Test
    fun crossfade_initialVariantTracked() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("crossfade_a").assertRecompositions(atLeast = 0)
    }

    @Test
    fun crossfade_cycleToVariantB_labelRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()

        onNodeWithTag("cycle_crossfade_btn").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("crossfade_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun crossfade_cycleToVariantB_variantBAppears() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()

        onNodeWithTag("cycle_crossfade_btn").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("crossfade_b").assertRecompositions(atLeast = 0)
    }

    @Test
    fun crossfade_siblingStable() = runComposeUiTest {
        setContent { DejavuTestContent { PagerCrossfadeScreen() } }
        waitForIdle()

        onNodeWithTag("cycle_crossfade_btn").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        refreshTagMapping()

        onNodeWithTag("pager_sibling").assertStable()
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
    BasicText("Page: $currentPage", Modifier.testTag("pager_indicator"))
}

@Composable
private fun CrossfadeVariantA() {
    BasicText("Variant A", Modifier.testTag("crossfade_a"))
}

@Composable
private fun CrossfadeVariantB() {
    BasicText("Variant B", Modifier.testTag("crossfade_b"))
}

@Composable
private fun CrossfadeVariantC() {
    BasicText("Variant C", Modifier.testTag("crossfade_c"))
}

@Composable
private fun CrossfadeLabel(target: Int) {
    BasicText("Crossfade: $target", Modifier.testTag("crossfade_label"))
}

@Composable
private fun StaticPagerSibling() {
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
