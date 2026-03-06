package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Cross-platform port of Android DialogPopupTest.
 * Validates Dialog/Popup composition tracking and
 * staticCompositionLocalOf vs compositionLocalOf invalidation behavior.
 */
@OptIn(ExperimentalTestApi::class)
class DialogPopupPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun dialog_contentTrackedWhenVisible() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        onNodeWithTag("show_dialog_btn").performClick()
        waitForIdle()

        onNodeWithTag("dialog_content").assertRecompositions(atLeast = 0)
    }

    @Test
    fun dialog_innerChildTracked() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        onNodeWithTag("show_dialog_btn").performClick()
        waitForIdle()

        onNodeWithTag("dialog_inner").assertRecompositions(atLeast = 0)
    }

    @Test
    fun dialog_dismissAndReshow_trackedCorrectly() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        // Show dialog
        onNodeWithTag("show_dialog_btn").performClick()
        waitForIdle()

        // Dismiss via button inside the dialog (replaces Espresso.pressBack())
        onNodeWithTag("dismiss_dialog_btn").performClick()
        waitForIdle()

        // Reset and show again
        resetRecompositionCounts()
        onNodeWithTag("show_dialog_btn").performClick()
        waitForIdle()

        // New dialog is fresh composition
        onNodeWithTag("dialog_content").assertRecompositions(atMost = 1)
    }

    @Test
    fun popup_contentTrackedWhenVisible() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        onNodeWithTag("show_popup_btn").performClick()
        waitForIdle()

        onNodeWithTag("popup_content").assertRecompositions(atLeast = 0)
    }

    @Test
    fun staticLocalChange_allReadersRecompose() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        onNodeWithTag("change_static_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_reader_a").assertRecompositions(exactly = 1)
        onNodeWithTag("static_reader_b").assertRecompositions(exactly = 1)
        onNodeWithTag("static_reader_c").assertRecompositions(exactly = 1)
    }

    @Test
    fun staticLocalChange_unrelatedChildAlsoRecomposes() = runComposeUiTest {
        // KEY: staticCompositionLocalOf invalidates ALL children in scope, even non-readers
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        onNodeWithTag("change_static_btn").performClick()
        waitForIdle()

        onNodeWithTag("unrelated_static").assertRecompositions(exactly = 1)
    }

    @Test
    fun dynamicChange_doesNotAffectStaticReaders() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        onNodeWithTag("change_dynamic_btn").performClick()
        waitForIdle()

        onNodeWithTag("dynamic_reader_d").assertRecompositions(exactly = 1)
        onNodeWithTag("static_reader_a").assertStable()
        onNodeWithTag("static_reader_b").assertStable()
        onNodeWithTag("static_reader_c").assertStable()
    }

    @Test
    fun multipleStaticChanges_countAccumulates() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        repeat(3) {
            onNodeWithTag("change_static_btn").performClick()
            waitForIdle()
        }

        onNodeWithTag("static_reader_a").assertRecompositions(exactly = 3)
        onNodeWithTag("static_reader_b").assertRecompositions(exactly = 3)
        onNodeWithTag("static_reader_c").assertRecompositions(exactly = 3)
        onNodeWithTag("unrelated_static").assertRecompositions(exactly = 3)
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

private val LocalStaticConfig = staticCompositionLocalOf { 0 }
private val LocalDynamicValue = compositionLocalOf { 0 }

@Composable
private fun DialogPopupScreen() {
    var staticConfig by remember { mutableIntStateOf(0) }
    var dynamicValue by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalStaticConfig provides staticConfig,
        LocalDynamicValue provides dynamicValue
    ) {
        Column {
            StaticReaderA()
            StaticReaderB()
            StaticReaderC()
            DynamicReaderD()
            UnrelatedStaticChild()
            if (showDialog) {
                Dialog(onDismissRequest = { showDialog = false }) {
                    DialogContent(onDismiss = { showDialog = false })
                }
            }
            if (showPopup) {
                Popup(onDismissRequest = { showPopup = false }) {
                    PopupContent()
                }
            }
            ShowDialogButton { showDialog = true }
            ShowPopupButton { showPopup = true }
            ChangeStaticButton { staticConfig++ }
            ChangeDynamicButton { dynamicValue++ }
        }
    }
}

@Composable
private fun StaticReaderA() {
    val config = LocalStaticConfig.current
    BasicText("StaticA: $config", Modifier.testTag("static_reader_a"))
}

@Composable
private fun StaticReaderB() {
    val config = LocalStaticConfig.current
    BasicText("StaticB: $config", Modifier.testTag("static_reader_b"))
}

@Composable
private fun StaticReaderC() {
    val config = LocalStaticConfig.current
    BasicText("StaticC: $config", Modifier.testTag("static_reader_c"))
}

@Composable
private fun DynamicReaderD() {
    val value = LocalDynamicValue.current
    BasicText("Dynamic: $value", Modifier.testTag("dynamic_reader_d"))
}

@Composable
private fun UnrelatedStaticChild() {
    BasicText("Unrelated", Modifier.testTag("unrelated_static"))
}

@Composable
private fun DialogContent(onDismiss: () -> Unit) {
    Column(Modifier.testTag("dialog_content")) {
        DialogInner()
        BasicText("Dismiss", Modifier.testTag("dismiss_dialog_btn").clickable { onDismiss() })
    }
}

@Composable
private fun DialogInner() {
    BasicText("Dialog Inner", Modifier.testTag("dialog_inner"))
}

@Composable
private fun PopupContent() {
    BasicText("Popup Content", Modifier.testTag("popup_content"))
}

@Composable
private fun ShowDialogButton(onClick: () -> Unit) {
    BasicText("Show Dialog", Modifier.testTag("show_dialog_btn").clickable { onClick() })
}

@Composable
private fun ShowPopupButton(onClick: () -> Unit) {
    BasicText("Show Popup", Modifier.testTag("show_popup_btn").clickable { onClick() })
}

@Composable
private fun ChangeStaticButton(onClick: () -> Unit) {
    BasicText("Change Static", Modifier.testTag("change_static_btn").clickable { onClick() })
}

@Composable
private fun ChangeDynamicButton(onClick: () -> Unit) {
    BasicText("Change Dynamic", Modifier.testTag("change_dynamic_btn").clickable { onClick() })
}
