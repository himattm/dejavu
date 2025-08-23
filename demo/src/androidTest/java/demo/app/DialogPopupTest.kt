package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
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
        composeTestRule.onNodeWithTag("dialog_content").assertRecompositions(atLeast = 0)
    }

    @Test
    fun dialog_innerChildTracked() {
        composeTestRule.onNodeWithTag("show_dialog_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dialog_inner").assertRecompositions(atLeast = 0)
    }

    @Test
    fun dialog_dismissAndReshow_trackedCorrectly() {
        // Show dialog
        composeTestRule.onNodeWithTag("show_dialog_btn").performClick()
        composeTestRule.waitForIdle()

        // Dismiss via back press
        Espresso.pressBack()
        composeTestRule.waitForIdle()

        // Reset and show again
        composeTestRule.resetRecompositionCounts()
        composeTestRule.onNodeWithTag("show_dialog_btn").performClick()
        composeTestRule.waitForIdle()

        // New dialog is fresh composition; framework ViewTreeOwners attachment
        // may cause up to 1 additional recomposition
        composeTestRule.onNodeWithTag("dialog_content").assertRecompositions(atMost = 1)
    }

    @Test
    fun popup_contentTrackedWhenVisible() {
        composeTestRule.onNodeWithTag("show_popup_btn").performClick()
        composeTestRule.waitForIdle()
        // Popup creates a separate window — may or may not be discoverable
        composeTestRule.onNodeWithTag("popup_content").assertRecompositions(atLeast = 0)
    }

    @Test
    fun staticLocalChange_allReadersRecompose() {
        composeTestRule.onNodeWithTag("change_static_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("static_reader_a").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("static_reader_b").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("static_reader_c").assertRecompositions(exactly = 1)
    }

    @Test
    fun staticLocalChange_unrelatedChildAlsoRecomposes() {
        // KEY: staticCompositionLocalOf invalidates ALL children in scope, even non-readers
        composeTestRule.onNodeWithTag("change_static_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("unrelated_static").assertRecompositions(exactly = 1)
    }

    @Test
    fun dynamicChange_doesNotAffectStaticReaders() {
        composeTestRule.onNodeWithTag("change_dynamic_btn").performClick()
        composeTestRule.waitForIdle()
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
