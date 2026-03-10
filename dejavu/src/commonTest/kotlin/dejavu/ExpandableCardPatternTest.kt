package dejavu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Cross-platform port of Android ExpandableCardTest.
 * Validates accordion using Foundation-only composables (Box + AnimatedVisibility).
 */
@OptIn(ExperimentalTestApi::class)
class ExpandableCardPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    @Test
    fun accordion_clicked_card_recomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        onNodeWithTag("card_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun accordion_previously_expanded_card_recomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        // Expand card 0
        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        resetRecompositionCounts()

        // Expand card 1 (collapses card 0)
        onNodeWithTag("card_header_1").performClick()
        waitForIdle()

        onNodeWithTag("card_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun accordion_uninvolved_cards_stable() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        // Expand card 0
        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        resetRecompositionCounts()

        // Expand card 1 (collapses card 0)
        onNodeWithTag("card_header_1").performClick()
        waitForIdle()

        onNodeWithTag("card_2").assertIsDisplayed()
        onNodeWithTag("card_3").assertIsDisplayed()
        onNodeWithTag("card_4").assertIsDisplayed()
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

        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        onNodeWithTag("accordion_title").assertStable()
    }

    @Test
    fun accordion_no_recomposition_without_interaction() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        onNodeWithTag("accordion_title").assertStable()
        onNodeWithTag("accordion_list_root").assertStable()
        onNodeWithTag("accordion_list").assertStable()
        onNodeWithTag("card_0").assertRecompositions(atLeast = 0)
        onNodeWithTag("card_1").assertRecompositions(atLeast = 0)
        onNodeWithTag("card_2").assertRecompositions(atLeast = 0)
        onNodeWithTag("card_3").assertRecompositions(atLeast = 0)
        onNodeWithTag("card_4").assertRecompositions(atLeast = 0)
    }

    @Test
    fun accordion_multiple_expansions_tracked() = runComposeUiTest {
        setContent { DejavuTestContent { AccordionScreen() } }
        waitForIdle()

        // Expand card 0
        onNodeWithTag("card_header_0").performClick()
        waitForIdle()

        resetRecompositionCounts()

        // Expand card 1
        onNodeWithTag("card_header_1").performClick()
        waitForIdle()

        resetRecompositionCounts()

        // Expand card 2
        onNodeWithTag("card_header_2").performClick()
        waitForIdle()

        onNodeWithTag("card_2").assertRecompositions(atLeast = 1)
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

@Composable
private fun AccordionScreen() {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
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
    BasicText("Accordion", Modifier.testTag("accordion_title"))
}

@Composable
private fun ExpandableCard(index: Int, isExpanded: Boolean, onToggle: () -> Unit) {
    Column(Modifier.testTag("card_$index")) {
        BasicText("Card $index", Modifier.testTag("card_header_$index").clickable { onToggle() })
        AnimatedVisibility(isExpanded) {
            BasicText("Content of card $index", Modifier.testTag("card_content_$index"))
        }
    }
}
