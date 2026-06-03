package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogPopupTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<DialogPopupActivity>()

    @Test
    fun dialog_contentTrackedWhenVisible() {
        composeTestRule.onNodeWithTag("show_dialog_btn").performClick()
        composeTestRule.waitForIdle()
        // Showing an Android Dialog attaches its own window + ViewTreeOwners to the sub-composition,
        // which recomposes the dialog content exactly once after its initial composition. Dejavu
        // counts that one recomposition (the non-Android common test has no real window, so there it
        // is 0 — see DialogPopupPatternTest).
        composeTestRule.onNodeWithTag("dialog_content").assertRecompositions(exactly = 1)
    }

    @Test
    fun dialog_innerChildTracked() {
        composeTestRule.onNodeWithTag("show_dialog_btn").performClick()
        composeTestRule.waitForIdle()
        // The inner child sits inside the dialog's window sub-composition, so it recomposes once on
        // the same ViewTreeOwners attachment pass that recomposes the dialog content (1 on Android;
        // 0 in the windowless common test).
        composeTestRule.onNodeWithTag("dialog_inner").assertRecompositions(exactly = 1)
    }

    @Test
    fun dialog_dismissAndReshow_trackedCorrectly() {
        // Show dialog
        composeTestRule.onNodeWithTag("show_dialog_btn").performClick()
        composeTestRule.waitForIdle()

        // Dismiss via dismiss button
        composeTestRule.onNodeWithTag("dismiss_dialog_btn").performClick()
        composeTestRule.waitForIdle()

        // Reset and show again
        composeTestRule.resetRecompositionCounts()
        composeTestRule.onNodeWithTag("show_dialog_btn").performClick()
        composeTestRule.waitForIdle()

        // The reshown dialog is a brand-new composition: the old DialogContent was disposed on
        // dismiss, and the fresh instance reuses the call site's compile-time key, so its first
        // post-reset composition counts as exactly one recomposition.
        composeTestRule.onNodeWithTag("dialog_content").assertRecompositions(exactly = 1)
    }

    @Test
    fun popup_contentTrackedWhenVisible() {
        composeTestRule.onNodeWithTag("show_popup_btn").performClick()
        composeTestRule.waitForIdle()
        // Popup content composes into a separate window; its first composition is not a
        // recomposition. With no further interaction after it is shown, it must be stable.
        composeTestRule.onNodeWithTag("popup_content").assertStable()
    }

    @Test
    fun staticLocalChange_allReadersRecompose() {
        composeTestRule.onNodeWithTag("change_static_btn").performClick()
        composeTestRule.onNodeWithTag("static_reader_a").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("static_reader_b").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("static_reader_c").assertRecompositions(exactly = 1)
    }

    @Test
    fun staticLocalChange_unrelatedChildAlsoRecomposes() {
        // KEY: staticCompositionLocalOf invalidates ALL children in scope, even non-readers
        composeTestRule.onNodeWithTag("change_static_btn").performClick()
        composeTestRule.onNodeWithTag("unrelated_static").assertRecompositions(exactly = 1)
    }

    @Test
    fun dynamicChange_doesNotAffectStaticReaders() {
        composeTestRule.onNodeWithTag("change_dynamic_btn").performClick()
        composeTestRule.onNodeWithTag("dynamic_reader_d").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("static_reader_a").assertStable()
        composeTestRule.onNodeWithTag("static_reader_b").assertStable()
        composeTestRule.onNodeWithTag("static_reader_c").assertStable()
    }

    @Test
    fun multipleStaticChanges_countAccumulates() {
        repeat(3) {
            composeTestRule.onNodeWithTag("change_static_btn").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("static_reader_a").assertRecompositions(exactly = 3)
        composeTestRule.onNodeWithTag("static_reader_b").assertRecompositions(exactly = 3)
        composeTestRule.onNodeWithTag("static_reader_c").assertRecompositions(exactly = 3)
        composeTestRule.onNodeWithTag("unrelated_static").assertRecompositions(exactly = 3)
    }
}
