@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package demo.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import demo.app.GroundTruthCounters

data class Product(val name: String, val category: String)

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

@Composable
fun ChipFilterScreen() {
  SideEffect { GroundTruthCounters.increment("chip_filter_root") }
  val selectedCategoriesState = remember { mutableStateOf(emptySet<String>()) }
  val selectedCategories = selectedCategoriesState.value

  val filteredProducts = if (selectedCategories.isEmpty()) {
    allProducts
  } else {
    allProducts.filter { it.category in selectedCategories }
  }

  val onToggle = remember<(String) -> Unit> {
    { category ->
      val current = selectedCategoriesState.value
      selectedCategoriesState.value = if (category in current) {
        current - category
      } else {
        current + category
      }
    }
  }

  Column(modifier = Modifier.testTag("chip_filter_root")) {
    ChipGroup(
      selectedCategories = selectedCategories,
      onToggle = onToggle,
    )
    Spacer(modifier = Modifier.height(8.dp))
    FilterCountLabel(count = filteredProducts.size)
    Spacer(modifier = Modifier.height(8.dp))
    FilteredList(products = filteredProducts)
    Spacer(modifier = Modifier.height(8.dp))
    ClearFiltersButton { selectedCategoriesState.value = emptySet() }
  }
}

@Composable
fun ChipGroup(
  selectedCategories: Set<String>,
  onToggle: (String) -> Unit,
) {
  SideEffect { GroundTruthCounters.increment("chip_group") }
  FlowRow(modifier = Modifier.testTag("chip_group")) {
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
fun FilterableChip(
  category: String,
  isSelected: Boolean,
  onToggle: (String) -> Unit,
  tag: String,
) {
  SideEffect { GroundTruthCounters.increment(tag) }
  val chipColor by animateColorAsState(
    targetValue = if (isSelected) Color(0xFF6200EE) else Color.LightGray,
    label = "chipColor_$tag"
  )
  Box(modifier = Modifier.testTag(tag).padding(end = 8.dp)) {
    FilterChip(
      selected = isSelected,
      onClick = { onToggle(category) },
      label = { Text(category) },
      colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = chipColor,
      ),
    )
  }
}

@Composable
fun FilteredList(products: List<Product>) {
  SideEffect { GroundTruthCounters.increment("filtered_list") }
  Column(modifier = Modifier.testTag("filtered_list")) {
    products.forEach { product ->
      val index = allProducts.indexOf(product)
      FilteredItem(name = product.name, tag = "filtered_item_$index")
    }
  }
}

@Composable
fun FilteredItem(name: String, tag: String) {
  SideEffect { GroundTruthCounters.increment(tag) }
  Text(
    text = name,
    modifier = Modifier.testTag(tag)
  )
}

@Composable
fun FilterCountLabel(count: Int) {
  SideEffect { GroundTruthCounters.increment("filter_count_label") }
  Text(
    text = "Showing $count items",
    modifier = Modifier.testTag("filter_count_label")
  )
}

@Composable
fun ClearFiltersButton(onClick: () -> Unit) {
  SideEffect { GroundTruthCounters.increment("clear_filters_btn") }
  Button(
    onClick = onClick,
    modifier = Modifier.testTag("clear_filters_btn")
  ) {
    Text("Clear Filters")
  }
}
