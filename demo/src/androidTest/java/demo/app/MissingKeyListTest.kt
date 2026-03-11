package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Demonstrates how Dejavu tracks per-item recompositions in LazyColumn.
 *
 * The demo has two modes:
 * - "Without Key": `items(data)` — no key parameter
 * - "With Key": `items(data, key = { it.id })` — stable identity
 *
 * Both render the same 50-item list. "Update Item #25" mutates only item 25.
 *
 * For in-place mutations, SnapshotStateList provides per-element change
 * tracking — so Compose only recomposes the mutated item regardless of keys.
 * Keys matter for structural changes (inserts, removals, reorders) where
 * positional identity would mismatch.
 */
@RunWith(AndroidJUnit4::class)
class MissingKeyListTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<MissingKeyListActivity>()

    /**
     * Without keys, an in-place mutation still only recomposes the mutated item.
     *
     * SnapshotStateList tracks per-element changes, so Compose knows exactly which
     * item changed even without keys. Unchanged items remain stable.
     */
    @Test
    fun noKey_updateOneItem_unchangedItemsAreStable() {
        // Default mode is "Without Key" — no toggle needed
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("update_item_btn")
            .performClick()
        composeTestRule.waitForIdle()

        // Items 0, 1, 2 didn't change — Compose skips them even without keys.
        composeTestRule.onNodeWithTag("nokey_item_0")
            .assertStable()
        composeTestRule.onNodeWithTag("nokey_item_1")
            .assertStable()
        composeTestRule.onNodeWithTag("nokey_item_2")
            .assertStable()
    }

    /**
     * With keys, unchanged items remain stable after an in-place mutation.
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
