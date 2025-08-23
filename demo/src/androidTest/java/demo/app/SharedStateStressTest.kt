package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedStateStressTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<SharedStateStressActivity>()

    @Test
    fun incrementA_onlyReaderARecomposes() {
        composeTestRule.onNodeWithTag("inc_a_btn").performClick()
        composeTestRule.onNodeWithTag("reader_a").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("reader_b").assertStable()
        composeTestRule.onNodeWithTag("reader_c").assertStable()
    }

    @Test
    fun incrementB_onlyReaderBRecomposes() {
        composeTestRule.onNodeWithTag("inc_b_btn").performClick()
        composeTestRule.onNodeWithTag("reader_b").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("reader_a").assertStable()
        composeTestRule.onNodeWithTag("reader_c").assertStable()
    }

    @Test
    fun incrementA_dualReaderRecomposes() {
        composeTestRule.onNodeWithTag("inc_a_btn").performClick()
        composeTestRule.onNodeWithTag("dual_reader").assertRecompositions(exactly = 1)
    }

    @Test
    fun incrementC_dualReaderStaysStable() {
        composeTestRule.onNodeWithTag("inc_c_btn").performClick()
        composeTestRule.onNodeWithTag("dual_reader").assertStable()
    }

    @Test
    fun toggleTheme_themeReaderRecomposes_countersStable() {
        composeTestRule.onNodeWithTag("toggle_theme_btn").performClick()
        composeTestRule.onNodeWithTag("theme_reader").assertRecompositions(exactly = 1)
        composeTestRule.onNodeWithTag("reader_a").assertStable()
        composeTestRule.onNodeWithTag("reader_b").assertStable()
        composeTestRule.onNodeWithTag("reader_c").assertStable()
    }

    @Test
    fun incrementAllThree_allReaderRecomposesThreeTimes() {
        composeTestRule.onNodeWithTag("inc_a_btn").performClick()
        composeTestRule.onNodeWithTag("inc_b_btn").performClick()
        composeTestRule.onNodeWithTag("inc_c_btn").performClick()
        composeTestRule.onNodeWithTag("all_reader").assertRecompositions(exactly = 3)
    }

    @Test
    fun incrementA_themeReaderStaysStable() {
        composeTestRule.onNodeWithTag("inc_a_btn").performClick()
        composeTestRule.onNodeWithTag("theme_reader").assertStable()
    }
}
