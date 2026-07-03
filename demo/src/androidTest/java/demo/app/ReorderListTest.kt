package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.getRecompositionCount
import dejavu.resetRecompositionCounts
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReorderListTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ReorderListActivity>()

    @Test
    fun reorder_swap_affects_first_two_items() {
        composeTestRule.onNodeWithTag("swap_first_two_btn").performClick()
        composeTestRule.waitForIdle()
        // Swapping items[0] and items[1] re-emits the loop with index 0↔1. Items are content-keyed
        // (key = item string), so the two swapped compositions persist but their `index` param flips,
        // changing the tag each now carries: after the swap reorderable_item_0 is the formerly-index-1
        // composition and reorderable_item_1 the formerly-index-0 one. Per-tag tracking (Android-only)
        // resolves the count by the tag currently present, so each of the two changed slots = 1.
        // 1: index param changed once for the composition now carrying this tag.
        composeTestRule.onNodeWithTag("reorderable_item_0").assertRecompositions(exactly = 1)
        // 1: index param changed once for the composition now carrying this tag.
        composeTestRule.onNodeWithTag("reorderable_item_1").assertRecompositions(exactly = 1)
    }

    @Test
    fun reorder_swap_other_items_stable() {
        composeTestRule.onNodeWithTag("swap_first_two_btn").performClick()
        composeTestRule.waitForIdle()
        // Only items 0 and 1 change index; items 2..5 keep their content key AND their index param,
        // so their compositions skip. Content-keyed lazy items do not over-recompose from a structural
        // swap (the keyed item identities are preserved), so each non-swapped slot is exactly stable.
        // 0: index and text unchanged for these slots — they skip.
        composeTestRule.onNodeWithTag("reorderable_item_2").assertStable()
        composeTestRule.onNodeWithTag("reorderable_item_3").assertStable()
        composeTestRule.onNodeWithTag("reorderable_item_4").assertStable()
        composeTestRule.onNodeWithTag("reorderable_item_5").assertStable()
    }

    @Test
    fun reorder_shuffle_affects_all_items() {
        // Per-tag tracking for LazyColumn items requires Group tree features
        // (identity, key-based counting) that are not fully reliable on
        // Compose 1.6.x (BOM 2024.06.00). Skip on old runtimes where
        // tag mapping for multi-instance lazy items may not resolve.
        assumeTrue(
            "Per-tag tracking in LazyColumn requires Compose 1.7+",
            isComposeRuntimeAtLeast(1, 7)
        )
        composeTestRule.onNodeWithTag("shuffle_btn").performClick()
        composeTestRule.waitForIdle()
        // KEEP: shuffle is a non-deterministic permutation. Items are content-keyed and the item only
        // recomposes when its `index` param changes, so whether the slot at reorderable_item_0 changed
        // index depends on where the shuffle landed its content — the per-item count is not fixed.
        composeTestRule.onNodeWithTag("reorderable_item_0").assertRecompositions(atLeast = 1)
    }

    @Test
    fun reorder_order_label_recomposes_on_mutation() {
        composeTestRule.onNodeWithTag("swap_first_two_btn").performClick()
        composeTestRule.waitForIdle()
        // The swap changes items.joinToString(", "), so the label's String param changes once and it
        // recomposes once. list_order_label is single-instance (one call site, unique key) so its
        // per-tag count resolves exactly on every platform.
        // 1: the joined order string changes exactly once on a single swap.
        composeTestRule.onNodeWithTag("list_order_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun reorder_reset_restores_order() {
        composeTestRule.onNodeWithTag("shuffle_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.resetRecompositionCounts()

        composeTestRule.onNodeWithTag("reset_order_btn").performClick()
        composeTestRule.waitForIdle()
        // Reset restores the original order string after a shuffle, changing the label's param once.
        // 1: the joined order string changes once when the list returns to its original order.
        // FLAG: non-deterministic edge case — if the prior shuffle happened to land on the original
        // order (1/720), the reset would be a same-value write and the label would skip (count 0).
        composeTestRule.onNodeWithTag("list_order_label").assertRecompositions(exactly = 1)
    }

    @Test
    fun reorder_static_label_stable() {
        composeTestRule.onNodeWithTag("shuffle_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("swap_first_two_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("static_reorder_label").assertStable()
    }

    @Test
    fun reorder_no_recomposition_without_interaction() {
        composeTestRule.onNodeWithTag("list_order_label").assertStable()
        composeTestRule.onNodeWithTag("static_reorder_label").assertStable()
        composeTestRule.onNodeWithTag("reorderable_item_0").assertStable()
    }

    companion object {
        /**
         * Returns true if the Compose runtime version is at least [major].[minor].
         * Uses reflection on the internal ComposeVersion class to avoid a compile-time
         * dependency on internal APIs. Returns true (optimistic) if version cannot be
         * determined.
         */
        private fun isComposeRuntimeAtLeast(major: Int, minor: Int): Boolean {
            return try {
                val clazz = Class.forName("androidx.compose.runtime.ComposeVersion")
                val field = clazz.getDeclaredField("version")
                field.isAccessible = true
                val version = field.getInt(null)
                // Compose version encoding: major * 1_000_000 + minor * 1_000 + patch
                // e.g., 1.6.8 = 10608, 1.7.0 = 10700
                // But the actual encoding in ComposeVersion is a monotonic build number:
                // Compose 1.6.x -> ~15xxx, 1.7.x -> ~16xxx
                // For simplicity, check a known threshold:
                // BOM 2024.06.00 (Compose 1.6.8) has ComposeVersion ~15407
                // BOM 2024.09.00 (Compose 1.7.0) has ComposeVersion ~16100
                // We require 1.7+, so check >= 16000
                val minVersion = when {
                    major == 1 && minor <= 6 -> 0
                    major == 1 && minor == 7 -> 16000
                    major == 1 && minor == 8 -> 17000
                    else -> major * 10000 + minor * 1000
                }
                version >= minVersion
            } catch (_: Throwable) {
                true // Optimistic: assume compatible if we can't determine version
            }
        }
    }
}
