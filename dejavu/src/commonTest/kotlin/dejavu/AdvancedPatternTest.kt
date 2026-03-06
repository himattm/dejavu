package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalTestApi::class)
class AdvancedPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun nestedLocal_changeOuter_outerReadersRecompose() = runComposeUiTest {
        setContent { DejavuTestContent { AdvancedScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_outer_btn").performClick()
        waitForIdle()

        onNodeWithTag("outer_reader").assertRecompositions(atLeast = 1)
        onNodeWithTag("outer_reader_b").assertRecompositions(atLeast = 1)
    }

    @Test
    fun nestedLocal_changeInner_innerReaderRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AdvancedScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_inner_btn").performClick()
        waitForIdle()

        onNodeWithTag("inner_reader").assertRecompositions(atLeast = 1)
    }

    @Test
    fun nestedLocal_changeInner_outerReadersStable() = runComposeUiTest {
        setContent { DejavuTestContent { AdvancedScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_inner_btn").performClick()
        waitForIdle()

        onNodeWithTag("outer_reader").assertStable()
        onNodeWithTag("outer_reader_b").assertStable()
    }

    @Test
    fun customLayout_childStableWhenNoStateChanges() = runComposeUiTest {
        setContent { DejavuTestContent { AdvancedScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_inner_btn").performClick()
        waitForIdle()

        onNodeWithTag("custom_layout_child").assertStable()
    }

    @Test
    fun rememberKey_changeKey_childRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AdvancedScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_key_btn_adv").performClick()
        waitForIdle()

        onNodeWithTag("remember_key_child").assertRecompositions(atLeast = 1)
    }

    @Test
    fun effectRestart_changeEffectKey_childRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { AdvancedScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_effect_key_btn").performClick()
        waitForIdle()

        onNodeWithTag("effect_restart").assertRecompositions(atLeast = 1)
    }
}

// ── Composables ──────────────────────────────────────────────────

private val LocalValue = compositionLocalOf { "default" }

@Composable
private fun AdvancedScreen() {
    var outerValue by remember { mutableIntStateOf(0) }
    var innerValue by remember { mutableIntStateOf(0) }
    var rememberKey by remember { mutableIntStateOf(0) }
    var effectKey by remember { mutableIntStateOf(0) }
    CompositionLocalProvider(LocalValue provides "outer-$outerValue") {
        Column {
            OuterReader()
            OuterReaderB()
            CompositionLocalProvider(LocalValue provides "inner-$innerValue") {
                InnerReader()
            }
            CustomLayoutChild()
            RememberKeyChild(rememberKey)
            EffectRestartChild(effectKey)
            BasicText("ChangeOuter", Modifier.testTag("change_outer_btn").clickable { outerValue++ })
            BasicText("ChangeInner", Modifier.testTag("change_inner_btn").clickable { innerValue++ })
            BasicText("ChangeKey", Modifier.testTag("change_key_btn_adv").clickable { rememberKey++ })
            BasicText("ChangeEffectKey", Modifier.testTag("change_effect_key_btn").clickable { effectKey++ })
        }
    }
}

@Composable
private fun OuterReader() {
    val v = LocalValue.current
    BasicText("Outer: $v", Modifier.testTag("outer_reader"))
}

@Composable
private fun OuterReaderB() {
    val v = LocalValue.current
    BasicText("OuterB: $v", Modifier.testTag("outer_reader_b"))
}

@Composable
private fun InnerReader() {
    val v = LocalValue.current
    BasicText("Inner: $v", Modifier.testTag("inner_reader"))
}

@Composable
private fun CustomLayoutChild() {
    BasicText("Custom", Modifier.testTag("custom_layout_child"))
}

@Composable
private fun RememberKeyChild(keyValue: Int) {
    val computed = remember(keyValue) { "computed-$keyValue" }
    BasicText("Remember: $computed", Modifier.testTag("remember_key_child"))
}

@Composable
private fun EffectRestartChild(effectKey: Int) {
    var effectRan by remember { mutableStateOf(false) }
    LaunchedEffect(effectKey) { effectRan = true }
    BasicText("Effect: $effectKey, ran=$effectRan", Modifier.testTag("effect_restart"))
}
