package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cross-platform port of Android DialogPopupTest.
 * Validates Dialog/Popup composition tracking and
 * staticCompositionLocalOf vs compositionLocalOf invalidation behavior.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect`. All tracked
 * composables here are single-instance (each emitted from one distinct call site — none from a
 * loop), so their per-tag counts resolve exactly on every platform → `exactly = delta(tag)`, with
 * a deterministic `assertEquals` documenting the expected number.
 *
 * Dialog/Popup content composes into a separate sub-composition/window; the public per-tag API
 * still tracks and counts it. Each test `waitForIdle()`s after opening/closing the dialog or popup
 * before asserting, and asserts on dialog/popup nodes only while they are open.
 */
@OptIn(ExperimentalTestApi::class)
class DialogPopupPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun dialog_contentTrackedWhenVisible() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        // Open the dialog (pre-interaction) so its content exists, then baseline.
        onNodeWithTag("show_dialog_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No further interaction after the baseline: the dialog content is tracked while visible
        // but must not recompose. tracer == ground truth proves the count is exactly right.
        onNodeWithTag("dialog_content")
            .assertRecompositions(exactly = GroundTruth.delta("dialog_content"))
        assertEquals(0, GroundTruth.delta("dialog_content"), "dialog content is stable while visible without interaction")
    }

    @Test
    fun dialog_innerChildTracked() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        // Open the dialog (pre-interaction) so the inner child exists, then baseline.
        onNodeWithTag("show_dialog_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No further interaction after the baseline: the inner child is tracked while visible but
        // must not recompose. tracer == ground truth proves the count is exactly right.
        onNodeWithTag("dialog_inner")
            .assertRecompositions(exactly = GroundTruth.delta("dialog_inner"))
        assertEquals(0, GroundTruth.delta("dialog_inner"), "dialog inner child is stable while visible without interaction")
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

        // Reset and baseline, then show again
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()
        onNodeWithTag("show_dialog_btn").performClick()
        waitForIdle()

        // The reshown dialog is a brand-new composition: the old DialogContent was disposed on
        // dismiss, and the fresh instance reuses the call site's compile-time key, so its
        // first post-baseline composition counts as exactly one recomposition. tracer == ground
        // truth proves the count is exactly right.
        onNodeWithTag("dialog_content")
            .assertRecompositions(exactly = GroundTruth.delta("dialog_content"))
        assertEquals(1, GroundTruth.delta("dialog_content"), "reshown dialog content composes exactly once after dismiss")
    }

    @Test
    fun popup_contentTrackedWhenVisible() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()

        // Open the popup (pre-interaction) so its content exists, then baseline.
        onNodeWithTag("show_popup_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        // No further interaction after the baseline: the popup content is tracked while visible
        // but must not recompose. tracer == ground truth proves the count is exactly right.
        onNodeWithTag("popup_content")
            .assertRecompositions(exactly = GroundTruth.delta("popup_content"))
        assertEquals(0, GroundTruth.delta("popup_content"), "popup content is stable while visible without interaction")
    }

    @Test
    fun staticLocalChange_allReadersRecompose() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("change_static_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_reader_a")
            .assertRecompositions(exactly = GroundTruth.delta("static_reader_a"))
        onNodeWithTag("static_reader_b")
            .assertRecompositions(exactly = GroundTruth.delta("static_reader_b"))
        onNodeWithTag("static_reader_c")
            .assertRecompositions(exactly = GroundTruth.delta("static_reader_c"))
        assertEquals(1, GroundTruth.delta("static_reader_a"), "static reader A recomposes once on static local change")
        assertEquals(1, GroundTruth.delta("static_reader_b"), "static reader B recomposes once on static local change")
        assertEquals(1, GroundTruth.delta("static_reader_c"), "static reader C recomposes once on static local change")
    }

    @Test
    fun staticLocalChange_unrelatedChildAlsoRecomposes() = runComposeUiTest {
        // KEY: staticCompositionLocalOf invalidates ALL children in scope, even non-readers
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("change_static_btn").performClick()
        waitForIdle()

        onNodeWithTag("unrelated_static")
            .assertRecompositions(exactly = GroundTruth.delta("unrelated_static"))
        assertEquals(1, GroundTruth.delta("unrelated_static"), "static local invalidates even non-reader children once")
    }

    @Test
    fun dynamicChange_doesNotAffectStaticReaders() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("change_dynamic_btn").performClick()
        waitForIdle()

        onNodeWithTag("dynamic_reader_d")
            .assertRecompositions(exactly = GroundTruth.delta("dynamic_reader_d"))
        assertEquals(1, GroundTruth.delta("dynamic_reader_d"), "dynamic reader recomposes once on dynamic local change")
        onNodeWithTag("static_reader_a").assertStable()
        onNodeWithTag("static_reader_b").assertStable()
        onNodeWithTag("static_reader_c").assertStable()
        assertEquals(0, GroundTruth.delta("static_reader_a"), "dynamic local change must not touch static reader A")
        assertEquals(0, GroundTruth.delta("static_reader_b"), "dynamic local change must not touch static reader B")
        assertEquals(0, GroundTruth.delta("static_reader_c"), "dynamic local change must not touch static reader C")
    }

    @Test
    fun multipleStaticChanges_countAccumulates() = runComposeUiTest {
        setContent { DejavuTestContent { DialogPopupScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        repeat(3) {
            onNodeWithTag("change_static_btn").performClick()
            waitForIdle()
        }

        onNodeWithTag("static_reader_a")
            .assertRecompositions(exactly = GroundTruth.delta("static_reader_a"))
        onNodeWithTag("static_reader_b")
            .assertRecompositions(exactly = GroundTruth.delta("static_reader_b"))
        onNodeWithTag("static_reader_c")
            .assertRecompositions(exactly = GroundTruth.delta("static_reader_c"))
        onNodeWithTag("unrelated_static")
            .assertRecompositions(exactly = GroundTruth.delta("unrelated_static"))
        assertEquals(3, GroundTruth.delta("static_reader_a"), "three static changes accumulate to three recompositions (A)")
        assertEquals(3, GroundTruth.delta("static_reader_b"), "three static changes accumulate to three recompositions (B)")
        assertEquals(3, GroundTruth.delta("static_reader_c"), "three static changes accumulate to three recompositions (C)")
        assertEquals(3, GroundTruth.delta("unrelated_static"), "three static changes accumulate to three recompositions (unrelated)")
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
    SideEffect { GroundTruth.record("static_reader_a") }
    val config = LocalStaticConfig.current
    BasicText("StaticA: $config", Modifier.testTag("static_reader_a"))
}

@Composable
private fun StaticReaderB() {
    SideEffect { GroundTruth.record("static_reader_b") }
    val config = LocalStaticConfig.current
    BasicText("StaticB: $config", Modifier.testTag("static_reader_b"))
}

@Composable
private fun StaticReaderC() {
    SideEffect { GroundTruth.record("static_reader_c") }
    val config = LocalStaticConfig.current
    BasicText("StaticC: $config", Modifier.testTag("static_reader_c"))
}

@Composable
private fun DynamicReaderD() {
    SideEffect { GroundTruth.record("dynamic_reader_d") }
    val value = LocalDynamicValue.current
    BasicText("Dynamic: $value", Modifier.testTag("dynamic_reader_d"))
}

@Composable
private fun UnrelatedStaticChild() {
    SideEffect { GroundTruth.record("unrelated_static") }
    BasicText("Unrelated", Modifier.testTag("unrelated_static"))
}

@Composable
private fun DialogContent(onDismiss: () -> Unit) {
    SideEffect { GroundTruth.record("dialog_content") }
    Column(Modifier.testTag("dialog_content")) {
        DialogInner()
        BasicText("Dismiss", Modifier.testTag("dismiss_dialog_btn").clickable { onDismiss() })
    }
}

@Composable
private fun DialogInner() {
    SideEffect { GroundTruth.record("dialog_inner") }
    BasicText("Dialog Inner", Modifier.testTag("dialog_inner"))
}

@Composable
private fun PopupContent() {
    SideEffect { GroundTruth.record("popup_content") }
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
