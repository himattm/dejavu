package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
class KeyIdentityPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun keyChange_treatedAsFreshComposition() = runComposeUiTest {
        setContent { DejavuTestContent { KeyIdentityScreen() } }
        waitForIdle()

        onNodeWithTag("change_key_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("change_key_btn").performClick()
        waitForIdle()

        onNodeWithTag("keyed_child").assertRecompositions(atLeast = 1)
    }

    @Test
    fun keyChange_thenLocalInc_countsFromZero() = runComposeUiTest {
        setContent { DejavuTestContent { KeyIdentityScreen() } }
        waitForIdle()

        onNodeWithTag("change_key_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("keyed_local_btn").performClick()
        waitForIdle()

        onNodeWithTag("keyed_child").assertRecompositions(exactly = 1)
    }

    @Test
    fun loopItems_initiallyStable() = runComposeUiTest {
        setContent { DejavuTestContent { LoopScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("loop_item_0").assertStable()
        onNodeWithTag("loop_item_1").assertStable()
        onNodeWithTag("loop_item_2").assertStable()
    }

    @Test
    fun addLoopItem_existingItemsShareCountWithNewItem() = runComposeUiTest {
        setContent { DejavuTestContent { LoopScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("add_loop_btn").performClick()
        waitForIdle()

        onNodeWithTag("loop_count_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun derivedChainA_recomposesWhenIntegerDivisionChanges() = runComposeUiTest {
        setContent { DejavuTestContent { DerivedChainScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        // base 0→1: derivedA = 0/2=0 → 1/2=0, no change
        onNodeWithTag("inc_base_btn").performClick()
        waitForIdle()
        onNodeWithTag("derived_chain_a").assertStable()

        resetRecompositionCounts()

        // base 1→2: derivedA = 1/2=0 → 2/2=1, changes
        onNodeWithTag("inc_base_btn").performClick()
        waitForIdle()
        onNodeWithTag("derived_chain_a").assertRecompositions(exactly = 1)
    }

    @Test
    fun derivedChainB_recomposesOnlyOnBooleanFlip() = runComposeUiTest {
        setContent { DejavuTestContent { DerivedChainScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        // base 0→2: derivedA = 0→1, derivedB = false→true
        onNodeWithTag("inc_base_twice_btn").performClick()
        waitForIdle()

        onNodeWithTag("derived_chain_b").assertRecompositions(exactly = 1)
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun KeyIdentityScreen() {
    var keyValue by remember { mutableIntStateOf(0) }
    var localCount by remember { mutableIntStateOf(0) }
    Column {
        key(keyValue) {
            KeyedChild(keyValue, localCount)
        }
        BasicText("ChangeKey", Modifier.testTag("change_key_btn").clickable { keyValue++ })
        BasicText("IncLocal", Modifier.testTag("keyed_local_btn").clickable { localCount++ })
    }
}

@Composable
private fun KeyedChild(keyVal: Int, local: Int) {
    BasicText("Keyed: $keyVal, local: $local", Modifier.testTag("keyed_child"))
}

@Composable
private fun LoopScreen() {
    var itemCount by remember { mutableIntStateOf(3) }
    Column {
        LoopCountLabel(itemCount)
        for (i in 0 until itemCount) {
            key(i) { LoopItem(i) }
        }
        BasicText("AddLoop", Modifier.testTag("add_loop_btn").clickable { itemCount++ })
    }
}

@Composable
private fun LoopCountLabel(count: Int) {
    BasicText("Count: $count", Modifier.testTag("loop_count_label"))
}

@Composable
private fun LoopItem(index: Int) {
    BasicText("Loop: $index", Modifier.testTag("loop_item_$index"))
}

@Composable
private fun DerivedChainScreen() {
    var baseValue by remember { mutableIntStateOf(0) }
    val derivedA by remember { derivedStateOf { baseValue / 2 } }
    val derivedB by remember { derivedStateOf { derivedA > 0 } }
    Column {
        DerivedChainA(derivedA)
        DerivedChainB(derivedB)
        BasicText("IncBase", Modifier.testTag("inc_base_btn").clickable { baseValue++ })
        BasicText("IncBase2x", Modifier.testTag("inc_base_twice_btn").clickable { baseValue += 2 })
    }
}

@Composable
private fun DerivedChainA(value: Int) {
    BasicText("DerivedA: $value", Modifier.testTag("derived_chain_a"))
}

@Composable
private fun DerivedChainB(value: Boolean) {
    BasicText("DerivedB: $value", Modifier.testTag("derived_chain_b"))
}
