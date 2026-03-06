package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
 * Cross-platform port of Android LazyListStressTest.
 * Validates LazyColumn with item selection, mutations, and resetCounts.
 */
@OptIn(ExperimentalTestApi::class)
class LazyListStressPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    @Test
    fun header_neverRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        onNodeWithTag("list_header").assertStable()
    }

    @Test
    fun selectOneItem_bannerRecomposesOnce() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        onNodeWithTag("selected_banner").assertRecompositions(exactly = 1)
    }

    @Test
    fun selectOneItem_derivedBannerRecomposesOnce() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        onNodeWithTag("derived_banner").assertRecompositions(exactly = 1)
    }

    @Test
    fun selectSecondItem_derivedBannerStaysStable() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        // Select first item (hasAnySelected goes from false to true)
        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        resetRecompositionCounts()

        // Select all (hasAnySelected stays true — derived value unchanged)
        onNodeWithTag("select_all_btn").performClick()
        waitForIdle()

        onNodeWithTag("derived_banner").assertStable()
    }

    @Test
    fun selectAll_listItemRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        onNodeWithTag("select_all_btn").performClick()
        waitForIdle()

        onNodeWithTag("item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun clearAfterSelectAll_listItemRecomposesAgain() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        onNodeWithTag("select_all_btn").performClick()
        waitForIdle()

        resetRecompositionCounts()

        onNodeWithTag("clear_btn").performClick()
        waitForIdle()

        onNodeWithTag("item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun tagMapping_lazyItems_resolveCorrectly() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        onNodeWithTag("item_0").assertRecompositions(atLeast = 0)
    }

    @Test
    fun selectOneItem_listItemRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { LazyListStressScreen() } }
        waitForIdle()

        onNodeWithTag("select_0_btn").performClick()
        waitForIdle()

        onNodeWithTag("item_0").assertRecompositions(atLeast = 1)
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
    BasicText("Header", Modifier.testTag("list_header"))
}

@Composable
private fun SelectedBanner(count: Int) {
    BasicText("Selected: $count", Modifier.testTag("selected_banner"))
}

@Composable
private fun DerivedBanner(hasAny: Boolean) {
    BasicText("HasAny: $hasAny", Modifier.testTag("derived_banner"))
}

@Composable
private fun ListItem(index: Int, isSelected: Boolean) {
    BasicText("Item $index sel=$isSelected", Modifier.testTag("item_$index"))
}
