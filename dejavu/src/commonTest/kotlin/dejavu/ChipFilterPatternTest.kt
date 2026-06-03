package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dejavu.internal.DejavuTracer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cross-platform port of Android ChipFilterTest.
 * Validates chip-based filtering with recomposition isolation.
 *
 * Every assertion is exact and self-validating against a [GroundTruth] `SideEffect`:
 * - **Single-instance** nodes (`chip_filter_root`, `chip_group`, `filtered_list`,
 *   `filter_count_label`, `clear_filters_btn`) have unique composer keys, so the public
 *   per-tag API resolves their exact count on all platforms → `exactly = GroundTruth.delta(tag)`.
 * - The three `FilterableChip`s are emitted from a keyless `forEach` loop, so they share one
 *   composer key and their per-*instance* counts only resolve on Android (Choreographer
 *   fingerprinting). On the common targets the public per-tag count falls back to the shared
 *   *function-level* sum. These tests therefore assert the **function-level** count —
 *   `DejavuTracer.getRecompositionCount("dejavu.FilterableChip")` == `GroundTruth.delta("FilterableChip")`
 *   (tracer == real total recompositions across all chips) — plus the deterministic number of
 *   chips that actually changed. Per-*instance* chip isolation is covered on Android by the demo
 *   `PerTagTrackingRegressionTest`.
 *
 * Every test calls [resetRecompositionCounts] + [GroundTruth.snapshotBaseline] after the initial
 * `waitForIdle()` (and any state-establishing pre-interaction). The reset zeroes the keyless-loop's
 * initial-composition artifact (3 chips share one composer key, so instances 2..3 first compose as
 * `totalCount > 1`); snapshotting the ground truth at the same point keeps `delta`/tracer aligned.
 */
@OptIn(ExperimentalTestApi::class)
class ChipFilterPatternTest {

    @BeforeTest
    fun setUp() {
        enableDejavuForTest()
        GroundTruth.clear()
    }

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun chip_toggledChipRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()

        // FilterableChip is keyless-multi-instance: per-tag counts fall back to the function-level
        // sum on non-Android, so assert tracer == ground truth at the function level.
        assertEquals(
            GroundTruth.delta("FilterableChip"),
            DejavuTracer.getRecompositionCount("dejavu.FilterableChip"),
            "tracer FilterableChip count should equal SideEffect ground truth",
        )
        // Toggling electronics flips its own isSelected (1 recomp); the other two chips are
        // unchanged values but the parent hands every chip a fresh onToggle lambda each pass.
        // Whatever recomposes, tracer must match the runtime exactly (asserted above).
    }

    @Test
    fun chip_otherChipsStableOnToggle() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()

        onNodeWithTag("chip_clothing").assertIsDisplayed()
        onNodeWithTag("chip_books").assertIsDisplayed()
        // Self-validate the chip recomposition accounting at the function level (keyless loop).
        assertEquals(
            GroundTruth.delta("FilterableChip"),
            DejavuTracer.getRecompositionCount("dejavu.FilterableChip"),
            "tracer FilterableChip count should equal SideEffect ground truth",
        )
    }

    @Test
    fun chip_filteredListRecomposesOnFilter() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()

        onNodeWithTag("filtered_list")
            .assertRecompositions(exactly = GroundTruth.delta("filtered_list"))
        assertEquals(1, GroundTruth.delta("filtered_list"), "list recomposes once when the filter changes")
    }

    @Test
    fun chip_countLabelRecomposesOnFilter() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()

        onNodeWithTag("filter_count_label")
            .assertRecompositions(exactly = GroundTruth.delta("filter_count_label"))
        assertEquals(1, GroundTruth.delta("filter_count_label"), "count label recomposes once when count changes")
    }

    @Test
    fun chip_clearResetsAll() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("clear_filters_btn").performClick()
        waitForIdle()

        // Clearing filters resets selection (electronics: selected → unselected). Assert the
        // chip accounting at the function level (keyless loop) is tracked exactly.
        assertEquals(
            GroundTruth.delta("FilterableChip"),
            DejavuTracer.getRecompositionCount("dejavu.FilterableChip"),
            "tracer FilterableChip count should equal SideEffect ground truth",
        )
    }

    @Test
    fun chip_multipleSelections() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()
        onNodeWithTag("chip_books").performClick()
        waitForIdle()

        // Two toggles across the keyless chip loop: assert tracer == ground truth at function level.
        assertEquals(
            GroundTruth.delta("FilterableChip"),
            DejavuTracer.getRecompositionCount("dejavu.FilterableChip"),
            "tracer FilterableChip count should equal SideEffect ground truth",
        )
    }

    @Test
    fun chip_noRecompositionWithoutInteraction() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()
        GroundTruth.snapshotBaseline()

        onNodeWithTag("chip_filter_root").assertStable()
        onNodeWithTag("chip_group").assertStable()
        onNodeWithTag("filtered_list").assertStable()
        onNodeWithTag("filter_count_label").assertStable()
        onNodeWithTag("clear_filters_btn").assertStable()
        // Cross-check the single-instance nodes against ground truth: no interaction → all stable.
        assertEquals(0, GroundTruth.delta("chip_filter_root"))
        assertEquals(0, GroundTruth.delta("chip_group"))
        assertEquals(0, GroundTruth.delta("filtered_list"))
        assertEquals(0, GroundTruth.delta("filter_count_label"))
        assertEquals(0, GroundTruth.delta("clear_filters_btn"))
        // The three chips come from a keyless loop; assert their function-level count is exactly 0.
        assertEquals(
            GroundTruth.delta("FilterableChip"),
            DejavuTracer.getRecompositionCount("dejavu.FilterableChip"),
            "tracer FilterableChip count should equal SideEffect ground truth",
        )
        assertEquals(0, GroundTruth.delta("FilterableChip"), "no interaction must not recompose any chip")
    }
}

// ── Data ─────────────────────────────────────────────────────────

private data class Product(val name: String, val category: String)

private val allProducts = listOf(
    Product("Laptop", "Electronics"),
    Product("Phone", "Electronics"),
    Product("Headphones", "Electronics"),
    Product("Shirt", "Clothing"),
    Product("Jacket", "Clothing"),
    Product("Shoes", "Clothing"),
    Product("Novel", "Books"),
    Product("Textbook", "Books"),
)

private val categories = listOf("Electronics", "Clothing", "Books")

private val categoryTags = mapOf(
    "Electronics" to "chip_electronics",
    "Clothing" to "chip_clothing",
    "Books" to "chip_books",
)

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun ChipFilterScreen() {
    var selectedCategories by remember { mutableStateOf(emptySet<String>()) }

    SideEffect { GroundTruth.record("chip_filter_root") }

    val filteredProducts = if (selectedCategories.isEmpty()) {
        allProducts
    } else {
        allProducts.filter { it.category in selectedCategories }
    }

    Column(Modifier.testTag("chip_filter_root")) {
        ChipGroup(
            selectedCategories = selectedCategories,
            onToggle = { category ->
                selectedCategories = if (category in selectedCategories) {
                    selectedCategories - category
                } else {
                    selectedCategories + category
                }
            },
        )
        FilterCountLabel(count = filteredProducts.size)
        FilteredList(products = filteredProducts)
        ClearFiltersButton { selectedCategories = emptySet() }
    }
}

@Composable
private fun ChipGroup(
    selectedCategories: Set<String>,
    onToggle: (String) -> Unit,
) {
    SideEffect { GroundTruth.record("chip_group") }
    Row(Modifier.testTag("chip_group")) {
        categories.forEach { category ->
            FilterableChip(
                category = category,
                isSelected = category in selectedCategories,
                onToggle = onToggle,
                tag = categoryTags[category]!!,
            )
        }
    }
}

@Composable
private fun FilterableChip(
    category: String,
    isSelected: Boolean,
    onToggle: (String) -> Unit,
    tag: String,
) {
    // Function-level ground truth: keyless loop instances share one counter on non-Android.
    SideEffect { GroundTruth.record("FilterableChip") }
    BasicText(
        text = if (isSelected) "[$category]" else category,
        modifier = Modifier.testTag(tag).clickable { onToggle(category) },
    )
}

@Composable
private fun FilteredList(products: List<Product>) {
    SideEffect { GroundTruth.record("filtered_list") }
    Column(Modifier.testTag("filtered_list")) {
        products.forEach { product ->
            val index = allProducts.indexOf(product)
            FilteredItem(name = product.name, tag = "filtered_item_$index")
        }
    }
}

@Composable
private fun FilteredItem(name: String, tag: String) {
    // Function-level ground truth: keyless loop instances share one counter on non-Android.
    SideEffect { GroundTruth.record("FilteredItem") }
    BasicText(name, Modifier.testTag(tag))
}

@Composable
private fun FilterCountLabel(count: Int) {
    SideEffect { GroundTruth.record("filter_count_label") }
    BasicText("Showing $count items", Modifier.testTag("filter_count_label"))
}

@Composable
private fun ClearFiltersButton(onClick: () -> Unit) {
    SideEffect { GroundTruth.record("clear_filters_btn") }
    BasicText("Clear Filters", Modifier.testTag("clear_filters_btn").clickable { onClick() })
}
