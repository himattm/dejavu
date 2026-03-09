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

/**
 * Demonstrates how Dejavu catches unnecessary recompositions caused by missing
 * `key` in LazyColumn's `items()`.
 *
 * The demo has two modes:
 * - "Without Key": `items(data)` — no key parameter (the anti-pattern)
 * - "With Key": `items(data, key = { it.id })` — stable identity (the fix)
 *
 * Both render the same 50-item list. "Update Item #25" mutates only item 25.
 */
@RunWith(AndroidJUnit4::class)
class MissingKeyListTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<MissingKeyListActivity>()

    /**
     * Proves the problem: without keys, changing ONE item recomposes ALL visible items.
     *
     * LazyColumn without a key function uses positional index as identity. When any
     * item in the backing list changes, Compose can't tell which items are actually
     * new — so it recomposes every visible slot. Dejavu makes this cost visible.
     */
    @Test
    fun noKey_updateOneItem_allVisibleItemsRecompose() {
        // Default mode is "Without Key" — no toggle needed
        composeTestRule.onNodeWithTag("update_item_btn")
            .performClick()

        // Only item 25 changed, but items 0, 1, 2 recompose too — that's the bug.
        composeTestRule.onNodeWithTag("nokey_item_0")
            .assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("nokey_item_1")
            .assertRecompositions(atLeast = 1)
        composeTestRule.onNodeWithTag("nokey_item_2")
            .assertRecompositions(atLeast = 1)
    }

    /**
     * Proves the fix: with keys, only the mutated item recomposes.
     *
     * When `items(data, key = { it.id })` provides stable identity, Compose knows
     * exactly which slot changed and skips recomposition for everything else.
     */
    @Test
    fun withKey_updateOneItem_unchangedItemsAreStable() {
        // Switch to "With Key" mode
        composeTestRule.onNodeWithTag("missing_key_toggle")
            .performClick()
        composeTestRule.waitForIdle()

        // Reset counts after the toggle so we only measure the update
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("update_item_btn")
            .performClick()

        // Items 0, 1, 2 didn't change — with keys, Dejavu confirms they're stable.
        composeTestRule.onNodeWithTag("keyed_item_0")
            .assertStable()
        composeTestRule.onNodeWithTag("keyed_item_1")
            .assertStable()
        composeTestRule.onNodeWithTag("keyed_item_2")
            .assertStable()
    }

    /**
     * The regression guard a developer would actually write.
     *
     * This test uses assertStable() to enforce that unchanged items don't recompose.
     * It passes with keys. If someone later removes the `key` parameter from items(),
     * Dejavu breaks the build — catching the regression before it ships.
     */
    @Test
    fun withKey_guardUnchangedItemsStable() {
        // Switch to "With Key" mode
        composeTestRule.onNodeWithTag("missing_key_toggle")
            .performClick()
        composeTestRule.waitForIdle()

        // Reset counts after the toggle so we only measure the update
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("update_item_btn")
            .performClick()

        // Item 0 is unchanged — with keys it should be stable
        composeTestRule.onNodeWithTag("keyed_item_0")
            .assertStable()
    }
}
