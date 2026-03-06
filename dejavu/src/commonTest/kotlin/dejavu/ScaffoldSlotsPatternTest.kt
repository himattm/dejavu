package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Cross-platform port of Android ScaffoldSlotsTest.
 * Validates slot-based recomposition isolation: changing state in one slot
 * should not cause recomposition in unrelated slots.
 */
@OptIn(ExperimentalTestApi::class)
class ScaffoldSlotsPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun changeTopBar_topBarRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { ScaffoldSlotsScreen() } }
        waitForIdle()

        onNodeWithTag("change_top_btn").performClick()
        waitForIdle()

        onNodeWithTag("scaffold_top_bar").assertRecompositions(exactly = 1)
    }

    @Test
    fun changeTopBar_bottomBarAndContentStable() = runComposeUiTest {
        setContent { DejavuTestContent { ScaffoldSlotsScreen() } }
        waitForIdle()

        onNodeWithTag("change_top_btn").performClick()
        waitForIdle()

        onNodeWithTag("scaffold_bottom_bar").assertStable()
        onNodeWithTag("content_body").assertStable()
    }

    @Test
    fun changeBottom_bottomBarRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { ScaffoldSlotsScreen() } }
        waitForIdle()

        onNodeWithTag("change_bottom_btn").performClick()
        waitForIdle()

        onNodeWithTag("scaffold_bottom_bar").assertRecompositions(exactly = 1)
    }

    @Test
    fun changeBottom_topBarStable() = runComposeUiTest {
        setContent { DejavuTestContent { ScaffoldSlotsScreen() } }
        waitForIdle()

        onNodeWithTag("change_bottom_btn").performClick()
        waitForIdle()

        onNodeWithTag("scaffold_top_bar").assertStable()
    }

    @Test
    fun changeContent_contentBodyRecomposes_staticStable() = runComposeUiTest {
        setContent { DejavuTestContent { ScaffoldSlotsScreen() } }
        waitForIdle()

        onNodeWithTag("change_content_btn").performClick()
        waitForIdle()

        onNodeWithTag("content_body").assertRecompositions(exactly = 1)
        onNodeWithTag("content_static").assertStable()
    }

    @Test
    fun clickFab_fabClickLabelRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { ScaffoldSlotsScreen() } }
        waitForIdle()

        onNodeWithTag("scaffold_fab").performClick()
        waitForIdle()

        onNodeWithTag("fab_click_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun clickFab_topAndBottomBarStable() = runComposeUiTest {
        setContent { DejavuTestContent { ScaffoldSlotsScreen() } }
        waitForIdle()

        onNodeWithTag("scaffold_fab").performClick()
        waitForIdle()

        onNodeWithTag("scaffold_top_bar").assertStable()
        onNodeWithTag("scaffold_bottom_bar").assertStable()
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun ScaffoldSlotsScreen() {
    var topBarTitle by remember { mutableStateOf("Title") }
    var bottomCount by remember { mutableIntStateOf(0) }
    var contentValue by remember { mutableIntStateOf(0) }
    var fabClickCount by remember { mutableIntStateOf(0) }

    Column {
        ScaffoldTopBar(topBarTitle)
        ScaffoldBottomBar(bottomCount)
        ScaffoldFab { fabClickCount++ }
        ScaffoldContent(
            contentValue = contentValue,
            fabClickCount = fabClickCount,
            onChangeTopBar = { topBarTitle = if (topBarTitle == "Title") "Changed" else "Title" },
            onChangeBottom = { bottomCount++ },
            onChangeContent = { contentValue++ },
        )
    }
}

@Composable
private fun ScaffoldTopBar(title: String) {
    Box {
        TopBarTitle(title)
    }
}

@Composable
private fun TopBarTitle(title: String) {
    BasicText(title, Modifier.testTag("scaffold_top_bar"))
}

@Composable
private fun ScaffoldBottomBar(count: Int) {
    Box {
        BottomBarContent(count)
    }
}

@Composable
private fun BottomBarContent(count: Int) {
    BasicText("Bottom: $count", Modifier.testTag("scaffold_bottom_bar"))
}

@Composable
private fun ScaffoldFab(onClick: () -> Unit) {
    BasicText("+", Modifier.testTag("scaffold_fab").clickable { onClick() })
}

@Composable
private fun ScaffoldContent(
    contentValue: Int,
    fabClickCount: Int,
    onChangeTopBar: () -> Unit,
    onChangeBottom: () -> Unit,
    onChangeContent: () -> Unit,
) {
    Column {
        ContentBody(contentValue)
        ContentStatic()
        FabClickLabel(fabClickCount)
        ChangeTopBarButton(onChangeTopBar)
        ChangeBottomButton(onChangeBottom)
        ChangeContentButton(onChangeContent)
    }
}

@Composable
private fun ContentBody(value: Int) {
    BasicText("Content: $value", Modifier.testTag("content_body"))
}

@Composable
private fun ContentStatic() {
    BasicText("Static content", Modifier.testTag("content_static"))
}

@Composable
private fun FabClickLabel(count: Int) {
    BasicText("FAB clicks: $count", Modifier.testTag("fab_click_label"))
}

@Composable
private fun ChangeTopBarButton(onClick: () -> Unit) {
    BasicText("Change Top Bar", Modifier.testTag("change_top_btn").clickable { onClick() })
}

@Composable
private fun ChangeBottomButton(onClick: () -> Unit) {
    BasicText("Change Bottom", Modifier.testTag("change_bottom_btn").clickable { onClick() })
}

@Composable
private fun ChangeContentButton(onClick: () -> Unit) {
    BasicText("Change Content", Modifier.testTag("change_content_btn").clickable { onClick() })
}
