package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Uses [SideEffect] as ground-truth to verify that DejavuTracer's
 * recomposition count matches actual compositions observed by the runtime.
 *
 * SideEffect runs after every successful composition (initial + recompositions),
 * so `sideEffectCount - 1 == dejavuRecompositionCount`.
 */
@OptIn(ExperimentalTestApi::class)
class SideEffectAccuracyTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        sideEffectCounter = 0
        childSideEffectCounter = 0
    }

    @AfterTest
    fun tearDown() {
        disableDejavuForTest()
    }

    @Test
    fun stableComposable_sideEffectRunsOnce_dejavuReportsZero() = runComposeUiTest {
        setContent {
            SideEffectCounter("Hello")
        }
        waitForIdle()

        val dejavuCount = DejavuTracer.getRecompositionCount("dejavu.SideEffectCounter")
        println("[stableComposable] SideEffect=$sideEffectCounter, Dejavu=$dejavuCount (expected: 1 / 0)")
        assertEquals(1, sideEffectCounter, "SideEffect should have run exactly once (initial composition)")
        assertEquals(0, dejavuCount, "Dejavu should report 0 recompositions for stable composable")
        assertEquals(sideEffectCounter - 1, dejavuCount,
            "Ground truth mismatch: SideEffect=$sideEffectCounter, Dejavu=$dejavuCount")
    }

    @Test
    fun singleStateChange_countsMatch() = runComposeUiTest {
        setContent {
            ClickCounter()
        }
        waitForIdle()

        val initialSideEffect = sideEffectCounter
        assertEquals(1, initialSideEffect, "Initial composition should trigger 1 SideEffect")

        onNodeWithTag("click_target").performClick()
        waitForIdle()

        val dejavuCount = DejavuTracer.getRecompositionCount("dejavu.ClickCounter")
        println("[singleStateChange] SideEffect=$sideEffectCounter, Dejavu=$dejavuCount (expected recomps: ${sideEffectCounter - 1})")
        assertEquals(sideEffectCounter - 1, dejavuCount,
            "After 1 click: SideEffect=$sideEffectCounter (${sideEffectCounter - 1} recomps), Dejavu=$dejavuCount")
    }

    @Test
    fun multipleStateChanges_countsMatch() = runComposeUiTest {
        setContent {
            ClickCounter()
        }
        waitForIdle()

        repeat(5) {
            onNodeWithTag("click_target").performClick()
            waitForIdle()
        }

        val dejavuCount = DejavuTracer.getRecompositionCount("dejavu.ClickCounter")
        val expectedRecomps = sideEffectCounter - 1
        println("[multipleStateChanges] SideEffect=$sideEffectCounter, Dejavu=$dejavuCount (expected recomps: $expectedRecomps)")
        assertEquals(expectedRecomps, dejavuCount,
            "After 5 clicks: SideEffect=$sideEffectCounter ($expectedRecomps recomps), Dejavu=$dejavuCount")
    }

    @Test
    fun childComposable_trackedIndependently() = runComposeUiTest {
        setContent {
            ParentWithChild()
        }
        waitForIdle()

        val parentInitial = sideEffectCounter
        val childInitial = childSideEffectCounter
        assertEquals(1, parentInitial)
        assertEquals(1, childInitial)

        // Click triggers parent state change — both parent and child should recompose
        onNodeWithTag("parent_click").performClick()
        waitForIdle()

        val parentDejavuCount = DejavuTracer.getRecompositionCount("dejavu.ParentWithChild")
        val childDejavuCount = DejavuTracer.getRecompositionCount("dejavu.TrackedChild")

        println("[childComposable] Parent: SideEffect=$sideEffectCounter, Dejavu=$parentDejavuCount | Child: SideEffect=$childSideEffectCounter, Dejavu=$childDejavuCount")
        assertEquals(sideEffectCounter - 1, parentDejavuCount,
            "Parent: SideEffect=$sideEffectCounter, Dejavu=$parentDejavuCount")
        assertEquals(childSideEffectCounter - 1, childDejavuCount,
            "Child: SideEffect=$childSideEffectCounter, Dejavu=$childDejavuCount")
    }

    @Test
    fun rapidStateChanges_countsStayAccurate() = runComposeUiTest {
        setContent {
            ClickCounter()
        }
        waitForIdle()

        repeat(20) {
            onNodeWithTag("click_target").performClick()
            waitForIdle()
        }

        val dejavuCount = DejavuTracer.getRecompositionCount("dejavu.ClickCounter")
        val expectedRecomps = sideEffectCounter - 1
        println("[rapidStateChanges] SideEffect=$sideEffectCounter, Dejavu=$dejavuCount (expected recomps: $expectedRecomps)")
        assertEquals(expectedRecomps, dejavuCount,
            "After 20 clicks: SideEffect=$sideEffectCounter ($expectedRecomps recomps), Dejavu=$dejavuCount")
    }
}

// ── Ground-truth counters ───────────────────────────────────────────

private var sideEffectCounter = 0
private var childSideEffectCounter = 0

// ── Test composables ────────────────────────────────────────────────

@Composable
private fun SideEffectCounter(message: String) {
    SideEffect { sideEffectCounter++ }
    BasicText(
        text = message,
        modifier = Modifier.testTag("side_effect_text")
    )
}

@Composable
private fun ClickCounter() {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { sideEffectCounter++ }
    Column(modifier = Modifier.testTag("click_counter")) {
        BasicText(text = "Count: $count")
        BasicText(
            text = "Click me",
            modifier = Modifier
                .testTag("click_target")
                .clickable { count++ }
        )
    }
}

@Composable
private fun ParentWithChild() {
    var parentState by remember { mutableIntStateOf(0) }
    SideEffect { sideEffectCounter++ }
    Column {
        BasicText(
            text = "Parent: $parentState",
            modifier = Modifier
                .testTag("parent_click")
                .clickable { parentState++ }
        )
        TrackedChild(value = parentState)
    }
}

@Composable
private fun TrackedChild(value: Int) {
    SideEffect { childSideEffectCounter++ }
    BasicText(
        text = "Child sees: $value",
        modifier = Modifier.testTag("child_text")
    )
}
