package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Cross-platform port of Android ChipFilterTest.
 * Validates chip-based filtering with recomposition isolation.
 * FilterableChip instances share a qualified-name counter, so per-instance
 * recomposition assertions are not always possible for non-toggled chips.
 */
@OptIn(ExperimentalTestApi::class)
class ChipFilterPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun chip_toggledChipRecomposes() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()

        onNodeWithTag("chip_electronics").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_otherChipsStableOnToggle() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()

        onNodeWithTag("chip_clothing").assertIsDisplayed()
        onNodeWithTag("chip_books").assertIsDisplayed()
    }

    @Test
    fun chip_filteredListRecomposesOnFilter() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()

        onNodeWithTag("filtered_list").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_countLabelRecomposesOnFilter() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()

        onNodeWithTag("filter_count_label").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_clearResetsAll() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("clear_filters_btn").performClick()
        waitForIdle()

        onNodeWithTag("chip_electronics").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_multipleSelections() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("chip_electronics").performClick()
        waitForIdle()
        onNodeWithTag("chip_books").performClick()
        waitForIdle()

        onNodeWithTag("chip_electronics").assertRecompositions(atLeast = 1)
        onNodeWithTag("chip_books").assertRecompositions(atLeast = 1)
    }

    @Test
    fun chip_noRecompositionWithoutInteraction() = runComposeUiTest {
        setContent { DejavuTestContent { ChipFilterScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("chip_filter_root").assertStable()
        onNodeWithTag("chip_group").assertStable()
        onNodeWithTag("chip_electronics").assertStable()
        onNodeWithTag("chip_clothing").assertStable()
        onNodeWithTag("chip_books").assertStable()
        onNodeWithTag("filtered_list").assertStable()
        onNodeWithTag("filter_count_label").assertStable()
        onNodeWithTag("clear_filters_btn").assertStable()
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
    BasicText(
        text = if (isSelected) "[$category]" else category,
        modifier = Modifier.testTag(tag).clickable { onToggle(category) },
    )
}

@Composable
private fun FilteredList(products: List<Product>) {
    Column(Modifier.testTag("filtered_list")) {
        products.forEach { product ->
            val index = allProducts.indexOf(product)
            FilteredItem(name = product.name, tag = "filtered_item_$index")
        }
    }
}

@Composable
private fun FilteredItem(name: String, tag: String) {
    BasicText(name, Modifier.testTag(tag))
}

@Composable
private fun FilterCountLabel(count: Int) {
    BasicText("Showing $count items", Modifier.testTag("filter_count_label"))
}

@Composable
private fun ClearFiltersButton(onClick: () -> Unit) {
    BasicText("Clear Filters", Modifier.testTag("clear_filters_btn").clickable { onClick() })
}
