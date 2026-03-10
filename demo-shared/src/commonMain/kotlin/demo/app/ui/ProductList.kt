package demo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import demo.app.GroundTruthCounters
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

// ============================================================
// ISSUE 1 — Overly broad parameter surface
// ProductHeader takes `selectedCount: Int` when it only needs
// `hasSelection: Boolean`. This causes a recomposition on every
// count change (0->1->2->3) instead of just the boolean flip
// (false->true). Three clicks = 3 recompositions instead of 1.
//
// OptimizedProductHeader is the "fixed" version that takes a
// Boolean, demonstrating the correct approach.
// ============================================================

// ============================================================
// ISSUE 2 — Unstable class parameter (identity-based equality)
// CartSummary is a regular class (NOT a data class). Its
// equals() uses reference identity (Object.equals). When the
// parent recomposes for ANY reason, a new CartSummary instance
// is created. Even if the logical content (itemCount,
// totalPrice) is identical, Compose sees a different reference
// and recomposes CartBanner.
//
// FIX: Make CartSummary a data class so equals() is structural.
// ============================================================

// ISSUE: Unstable class — uses identity-based equality (Object.equals),
// causing recomposition even when the logical content is the same.
// Making this a `data class` would fix the problem.
class CartSummary(val itemCount: Int, val totalPrice: String)

@Composable
fun ProductListScreen() {
    SideEffect { GroundTruthCounters.increment("product_list_screen") }
    var selectedCount by remember { mutableIntStateOf(0) }
    var refreshCount by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.testTag("product_list_screen")) {
        // ISSUE 1: Header reads selectedCount as Int, recomposes on every count change
        ProductHeader(selectedCount = selectedCount, tag = "product_header")

        // FIXED version: only recomposes when the boolean flips (false->true or true->false)
        OptimizedProductHeader(
            hasSelection = selectedCount > 0,
            tag = "optimized_header",
        )

        // ISSUE 2: New CartSummary instance created every recomposition of ProductListScreen.
        // Because CartSummary is NOT a data class, each instance is != the previous
        // even when itemCount and totalPrice hold the same values.
        // This means CartBanner recomposes on EVERY parent recomposition --
        // including refreshCount changes that don't affect the cart at all.
        //
        // KEY: refreshCount is read in ProductListScreen's scope (see
        // RefreshIndicator below), so changing refreshCount recomposes this
        // entire composable. That creates a new CartSummary with the SAME
        // selectedCount, but because CartSummary uses identity equality,
        // CartBanner sees a "different" parameter and recomposes needlessly.
        CartBanner(
            summary = CartSummary(
                itemCount = selectedCount,
                totalPrice = "$${selectedCount * 9.99}",
            ),
            tag = "cart_banner",
        )

        // Well-behaved: only recomposes when its own data changes
        ProductItem(
            name = "Widget A",
            price = "$9.99",
            selected = selectedCount > 0,  // derived boolean
            onToggle = { if (it) selectedCount++ else selectedCount-- },
            tag = "product_a",
        )

        ProductItem(
            name = "Widget B",
            price = "$19.99",
            selected = selectedCount > 1,
            onToggle = { if (it) selectedCount++ else selectedCount-- },
            tag = "product_b",
        )

        // NOTE ON LAMBDA STABILITY: With Compose 1.7+ strong skipping mode,
        // inline lambdas are automatically memoized by the compiler. The lambda
        // { refreshCount++ } is effectively wrapped in remember { ... }, so
        // ProductFooter does NOT recompose when the parent recomposes.
        // This is NOT a visible issue in practice -- it is masked by strong skipping.
        ProductFooter(
            onRefresh = { refreshCount++ },
            tag = "product_footer",
        )

        // Expected recomposition: should recompose once per selection change
        TotalDisplay(count = selectedCount, tag = "total_display")

        // RefreshIndicator reads `refreshCount` in ProductListScreen's scope.
        // This ensures that incrementing refreshCount recomposes the entire
        // ProductListScreen body, which is required to demonstrate Issue 2
        // (CartSummary identity instability on unrelated parent recomposition).
        RefreshIndicator(refreshCount = refreshCount, tag = "refresh_indicator")

        // Control buttons
        Button(
            onClick = { selectedCount++ },
            modifier = Modifier.testTag("select_button"),
        ) { Text("Select") }

        Button(
            onClick = { refreshCount++ },
            modifier = Modifier.testTag("refresh_button"),
        ) { Text("Refresh") }
    }
}

// ── ISSUE 1: Overly broad Int parameter ────────────────────
// Recomposes on EVERY selectedCount change even though it only
// needs to know if count > 0 for the badge text.
@Composable
fun ProductHeader(selectedCount: Int, tag: String) {
    SideEffect { GroundTruthCounters.increment(tag) }
    val badge = if (selectedCount > 0) " ($selectedCount selected)" else ""
    Text(
        text = "Products$badge",
        modifier = Modifier.testTag(tag),
    )
}

// ── FIXED: Boolean parameter — only recomposes on the flip ──
// Takes `hasSelection: Boolean` instead of the raw count.
// Three consecutive selections (0->1->2->3) only cause ONE
// recomposition here (false->true), versus 3 for ProductHeader.
@Composable
fun OptimizedProductHeader(hasSelection: Boolean, tag: String) {
    SideEffect { GroundTruthCounters.increment(tag) }
    val badge = if (hasSelection) " (items selected)" else ""
    Text(
        text = "Products$badge",
        modifier = Modifier.testTag(tag),
    )
}

// ── ISSUE 2: Unstable class parameter ───────────────────────
// CartSummary uses reference equality, so a new instance always
// triggers recomposition — even if the values inside are identical.
@Composable
fun CartBanner(summary: CartSummary, tag: String) {
    SideEffect { GroundTruthCounters.increment(tag) }
    Text(
        text = "${summary.itemCount} items - ${summary.totalPrice}",
        modifier = Modifier.testTag(tag),
    )
}

// Well-behaved composable: Boolean parameter, only recomposes on actual change
@Composable
fun ProductItem(
    name: String,
    price: String,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    tag: String,
) {
    SideEffect { GroundTruthCounters.increment(tag) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable { onToggle(!selected) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = onToggle)
        Text("$name - $price", modifier = Modifier.padding(start = 8.dp))
    }
}

// NOTE: The inline lambda issue is masked by strong skipping mode (Compose 1.7+).
// With strong skipping, the compiler auto-memoizes lambdas, so this does NOT
// recompose when the parent recomposes. Without strong skipping, the inline lambda
// would cause a recomposition on every parent recomposition.
@Composable
fun ProductFooter(onRefresh: () -> Unit, tag: String) {
    SideEffect { GroundTruthCounters.increment(tag) }
    Button(
        onClick = onRefresh,
        modifier = Modifier.testTag(tag),
    ) {
        Text("Refresh Products")
    }
}

// Expected recomposition: should recompose once per selection change
@Composable
fun TotalDisplay(count: Int, tag: String) {
    SideEffect { GroundTruthCounters.increment(tag) }
    Text(
        text = "Total selected: $count",
        modifier = Modifier.testTag(tag),
    )
}

// Shows the refresh count. This composable reads refreshCount as a
// parameter, which forces ProductListScreen to recompose when
// refreshCount changes. Without this read, Compose's fine-grained
// invalidation would NOT recompose ProductListScreen on refresh
// clicks (because refreshCount would only be read inside memoized
// lambdas), and the CartSummary instability issue wouldn't be visible.
@Composable
fun RefreshIndicator(refreshCount: Int, tag: String) {
    SideEffect { GroundTruthCounters.increment(tag) }
    Text(
        text = "Refreshed $refreshCount times",
        modifier = Modifier.testTag(tag),
    )
}
