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
 * Cross-platform port of Android SharedStateStressTest.
 * Validates multiple independent state readers and CompositionLocal separation.
 */
@OptIn(ExperimentalTestApi::class)
class SharedStateStressPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    @Test
    fun incrementA_onlyReaderARecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { SharedStateScreen() } }
        waitForIdle()

        onNodeWithTag("inc_a_btn").performClick()
        waitForIdle()

        onNodeWithTag("reader_a").assertRecompositions(exactly = 1)
        onNodeWithTag("reader_b").assertStable()
        onNodeWithTag("reader_c").assertStable()
    }

    @Test
    fun incrementB_onlyReaderBRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { SharedStateScreen() } }
        waitForIdle()

        onNodeWithTag("inc_b_btn").performClick()
        waitForIdle()

        onNodeWithTag("reader_b").assertRecompositions(exactly = 1)
        onNodeWithTag("reader_a").assertStable()
        onNodeWithTag("reader_c").assertStable()
    }

    @Test
    fun incrementA_dualReaderRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { SharedStateScreen() } }
        waitForIdle()

        onNodeWithTag("inc_a_btn").performClick()
        waitForIdle()

        onNodeWithTag("dual_reader").assertRecompositions(exactly = 1)
    }

    @Test
    fun incrementC_dualReaderStaysStable() = runComposeUiTest {
        setContent { DejavuTestContent { SharedStateScreen() } }
        waitForIdle()

        onNodeWithTag("inc_c_btn").performClick()
        waitForIdle()

        onNodeWithTag("dual_reader").assertStable()
    }

    @Test
    fun toggleTheme_themeReaderRecomposes_countersStable() = runComposeUiTest {
        setContent { DejavuTestContent { SharedStateScreen() } }
        waitForIdle()

        onNodeWithTag("toggle_theme_btn").performClick()
        waitForIdle()

        onNodeWithTag("theme_reader").assertRecompositions(exactly = 1)
        onNodeWithTag("reader_a").assertStable()
        onNodeWithTag("reader_b").assertStable()
        onNodeWithTag("reader_c").assertStable()
    }

    @Test
    fun incrementAllThree_allReaderRecomposesThreeTimes() = runComposeUiTest {
        setContent { DejavuTestContent { SharedStateScreen() } }
        waitForIdle()

        onNodeWithTag("inc_a_btn").performClick()
        waitForIdle()
        onNodeWithTag("inc_b_btn").performClick()
        waitForIdle()
        onNodeWithTag("inc_c_btn").performClick()
        waitForIdle()

        onNodeWithTag("all_reader").assertRecompositions(exactly = 3)
    }

    @Test
    fun incrementA_themeReaderStaysStable() = runComposeUiTest {
        setContent { DejavuTestContent { SharedStateScreen() } }
        waitForIdle()

        onNodeWithTag("inc_a_btn").performClick()
        waitForIdle()

        onNodeWithTag("theme_reader").assertStable()
    }
}

// ══════════════════════════════════════════════════════════════
// Composables
// ══════════════════════════════════════════════════════════════

private val LocalTheme = compositionLocalOf { "light" }

@Composable
private fun SharedStateScreen() {
    var a by remember { mutableIntStateOf(0) }
    var b by remember { mutableIntStateOf(0) }
    var c by remember { mutableIntStateOf(0) }
    var theme by remember { mutableStateOf("light") }
    CompositionLocalProvider(LocalTheme provides theme) {
        Column {
            ReaderA(a)
            ReaderB(b)
            ReaderC(c)
            DualReader(a, b)
            AllReader(a + b + c)
            ThemeReader()
            BasicText("IncA", Modifier.testTag("inc_a_btn").clickable { a++ })
            BasicText("IncB", Modifier.testTag("inc_b_btn").clickable { b++ })
            BasicText("IncC", Modifier.testTag("inc_c_btn").clickable { c++ })
            BasicText("ToggleTheme", Modifier.testTag("toggle_theme_btn").clickable {
                theme = if (theme == "light") "dark" else "light"
            })
        }
    }
}

@Composable
private fun ReaderA(value: Int) {
    BasicText("A: $value", Modifier.testTag("reader_a"))
}

@Composable
private fun ReaderB(value: Int) {
    BasicText("B: $value", Modifier.testTag("reader_b"))
}

@Composable
private fun ReaderC(value: Int) {
    BasicText("C: $value", Modifier.testTag("reader_c"))
}

@Composable
private fun DualReader(a: Int, b: Int) {
    BasicText("A+B: ${a + b}", Modifier.testTag("dual_reader"))
}

@Composable
private fun AllReader(total: Int) {
    BasicText("All: $total", Modifier.testTag("all_reader"))
}

@Composable
private fun ThemeReader() {
    val t = LocalTheme.current
    BasicText("Theme: $t", Modifier.testTag("theme_reader"))
}
