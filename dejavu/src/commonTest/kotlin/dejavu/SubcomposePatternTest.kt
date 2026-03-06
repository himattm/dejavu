package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SubcomposePatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun constraintChange_recomposesSubcomposition() = runComposeUiTest {
        setContent { DejavuTestContent { SubcomposeScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("toggle_width_btn").performClick()
        waitForIdle()

        onNodeWithTag("constraint_reader").assertRecompositions(atLeast = 1)
    }

    @Test
    fun noConstraintChange_subcompositionStable() = runComposeUiTest {
        setContent { DejavuTestContent { SubcomposeScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("trigger_parent_btn").performClick()
        waitForIdle()

        onNodeWithTag("constraint_reader").assertStable()
    }

    @Test
    fun moveContent_preservesState_noRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { MovableScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("move_content_btn").performClick()
        waitForIdle()

        onNodeWithTag("movable_child").assertStable()
    }

    @Test
    fun moveContentTwice_backToOriginalSlot_stillStable() = runComposeUiTest {
        setContent { DejavuTestContent { MovableScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("move_content_btn").performClick()
        waitForIdle()
        onNodeWithTag("move_content_btn").performClick()
        waitForIdle()

        onNodeWithTag("movable_child").assertStable()
    }

    @Test
    fun nonRestartable_recomposesWithParent() = runComposeUiTest {
        setContent { DejavuTestContent { SubcomposeScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("trigger_parent_btn").performClick()
        waitForIdle()

        onNodeWithTag("non_restartable").assertRecompositions(atLeast = 1)
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun SubcomposeScreen() {
    var wide by remember { mutableStateOf(false) }
    var trigger by remember { mutableIntStateOf(0) }
    Column {
        Box(Modifier.let { if (wide) it.fillMaxWidth() else it.width(100.dp) }) {
            BoxWithConstraints {
                ConstraintReader(maxWidth.value.toInt())
            }
        }
        RegularChild(trigger)
        NonRestartableChild(trigger)
        BasicText("ToggleWidth", Modifier.testTag("toggle_width_btn").clickable { wide = !wide })
        BasicText("TriggerParent", Modifier.testTag("trigger_parent_btn").clickable { trigger++ })
    }
}

@Composable
private fun ConstraintReader(maxWidthPx: Int) {
    BasicText("Width: $maxWidthPx", Modifier.testTag("constraint_reader"))
}

@Composable
private fun RegularChild(value: Int) {
    BasicText("Regular: $value", Modifier.testTag("regular_child"))
}

@NonRestartableComposable
@Composable
private fun NonRestartableChild(value: Int) {
    BasicText("NonRestart: $value", Modifier.testTag("non_restartable"))
}

@Composable
private fun MovableScreen() {
    var inSlotA by remember { mutableStateOf(true) }
    val movable = remember { movableContentOf { MovableChild() } }
    Column {
        if (inSlotA) {
            Box(Modifier.testTag("slot_a")) { movable() }
        } else {
            Box(Modifier.testTag("slot_b")) { movable() }
        }
        BasicText("Move", Modifier.testTag("move_content_btn").clickable { inSlotA = !inSlotA })
    }
}

@Composable
private fun MovableChild() {
    BasicText("Movable", Modifier.testTag("movable_child"))
}
