package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test for issue #80: `DejavuComposeTestRule.apply()` must work when the
 * test body is wrapped in `kotlinx.coroutines.test.runTest`.
 *
 * Without the fix in `DejavuComposeTestRule`, setup runs on a `TestDispatcher` thread
 * (no `Looper`) and crashes with `IllegalStateException: The current thread must
 * have a looper!` from `Choreographer.getInstance()`.
 */
@RunWith(AndroidJUnit4::class)
class RunTestRuleSetupTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<CounterActivity>()

    @Test
    fun ruleSetup_succeedsInsideRunTest() = runTest {
        composeTestRule.onNodeWithTag("counter_value").assertStable()
    }
}
