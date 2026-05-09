package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecompositionRegressionTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ProductActivity>()

    // ============================================================
    // PASSING TESTS: Assert expected (correct) recomposition counts
    // These represent the "baseline" that should not regress.
    // ============================================================

    @Test
    fun wellBehavedItem_recomposesOncePerStateChange() {
        // Click select once -- product_a depends on `selectedCount > 0` (false -> true)
        composeTestRule.onNodeWithTag("select_button").performClick()

        // TotalDisplay should recompose exactly once (its count param changed once)
        composeTestRule.onNodeWithTag("total_display").assertRecompositions(exactly = 1)
    }

    @Test
    fun stableComponent_doesNotRecomposeOnUnrelatedChange() {
        // Click refresh -- this changes refreshCount but NOT selectedCount
        composeTestRule.onNodeWithTag("refresh_button").performClick()

        // TotalDisplay depends only on selectedCount, not refreshCount.
        // When ProductListScreen recomposes (due to refreshCount change),
        // it passes selectedCount=0 to TotalDisplay -- same value as before.
        // Compose skips the recomposition because the parameter is unchanged.
        composeTestRule.onNodeWithTag("total_display").assertStable()
    }

    @Test
    fun multipleSelections_tracksCorrectly() {
        // Click select 3 times
        repeat(3) {
            composeTestRule.onNodeWithTag("select_button").performClick()
        }

        // TotalDisplay should recompose at least once per click (at least 3 times)
        composeTestRule.onNodeWithTag("total_display").assertRecompositions(atLeast = 3)
    }

    // ============================================================
    // REGRESSION DETECTION: Overly broad parameter surface (Issue 1)
    //
    // ProductHeader takes `selectedCount: Int` when it only needs
    // `hasSelection: Boolean`. This means 3 clicks cause 3
    // recompositions (0->1, 1->2, 2->3) instead of just 1
    // (false->true).
    //
    // OptimizedProductHeader demonstrates the fix.
    // ============================================================

    @Test
    fun header_recomposesOnEveryCountChange() {
        // CURRENT BEHAVIOR (issue): ProductHeader recomposes 3 times
        // because selectedCount changes 3 times (0->1->2->3).
        repeat(3) {
            composeTestRule.onNodeWithTag("select_button").performClick()
        }

        // ISSUE: 3 recompositions for 3 clicks -- one per Int change
        composeTestRule.onNodeWithTag("product_header").assertRecompositions(atLeast = 3)

        // FIX: If ProductHeader took `hasSelection: Boolean` instead of
        // `selectedCount: Int`, it would only recompose ONCE (false->true).
        // The fixed assertion would be: assertRecomposesExactly(1)
    }

    @Test
    fun optimizedHeader_recomposesOnlyOnBooleanFlip() {
        // FIXED BEHAVIOR: OptimizedProductHeader takes `hasSelection: Boolean`.
        // Three clicks change selectedCount 0->1->2->3, but the derived
        // boolean only flips once: false->true. So only 1 recomposition.
        repeat(3) {
            composeTestRule.onNodeWithTag("select_button").performClick()
        }

        // Only 1 recomposition: the boolean flip from false to true
        composeTestRule.onNodeWithTag("optimized_header").assertRecompositions(exactly = 1)
    }

    // ============================================================
    // REGRESSION DETECTION: Unstable class parameter (Issue 2)
    //
    // CartSummary is a regular class (not a data class). Its
    // equals() is identity-based (Object.equals), so a new
    // instance is always != the old instance even if the fields
    // are identical. When the parent recomposes for ANY reason,
    // CartBanner gets a new CartSummary reference and recomposes.
    //
    // FIX: Make CartSummary a `data class`. Then equals() is
    // structural and Compose can skip when content is unchanged.
    // ============================================================

    @Test
    fun cartBanner_recomposesOnUnrelatedRefresh() {
        // CURRENT BEHAVIOR (issue): Clicking refresh changes refreshCount,
        // which recomposes ProductListScreen. A new CartSummary(0, "$0.0")
        // is created. Because CartSummary is NOT a data class, the new
        // instance != the old instance (reference inequality), so
        // CartBanner recomposes even though the cart content is unchanged.
        composeTestRule.onNodeWithTag("refresh_button").performClick()

        // ISSUE: CartBanner recomposes despite no logical change to the cart

        // FIX: CartSummary is a `data class`, equals() compares
        // fields structurally. CartSummary(0, "$0.0") == CartSummary(0, "$0.0")
        // is true, and Compose SKIPS the recomposition.
        composeTestRule.onNodeWithTag("cart_banner").assertStable()
    }

    // ============================================================
    // LAMBDA STABILITY: Strong skipping masks this issue (Compose 1.7+)
    // The inline lambda { refreshCount++ } is auto-memoized, so
    // ProductFooter does NOT recompose on parent recomposition.
    // This test documents the CURRENT (stable) behavior.
    // ============================================================

    @Test
    fun footer_staysStableDueToStrongSkipping() {
        // Click select -- recomposes ProductListScreen, which recreates
        // the inline lambda { refreshCount++ } passed to ProductFooter.
        // With strong skipping, the lambda is auto-memoized.
        composeTestRule.onNodeWithTag("select_button").performClick()

        // Strong skipping auto-memoizes lambdas -- footer stays stable.
        // If strong skipping were disabled (e.g., via compiler flag),
        // this would FAIL and the footer would recompose on every parent
        // recomposition due to the unstable lambda reference.
        composeTestRule.onNodeWithTag("product_footer").assertStable()
    }

    // ============================================================
    // PERFORMANCE BUDGET TESTS
    // ============================================================

    @Test
    fun performanceBudget_cartBannerOnMixedInteractions() {
        // Mixed interactions: 2 selects + 3 refreshes
        repeat(2) {
            composeTestRule.onNodeWithTag("select_button").performClick()
        }
        repeat(3) {
            composeTestRule.onNodeWithTag("refresh_button").performClick()
        }

        // CURRENT BEHAVIOR (issue): CartBanner recomposes on EVERY parent
        // recomposition because CartSummary is unstable. That's 5 total
        // recompositions (2 from selects + 3 from refreshes).
        // Budget: at most 5 -- bounded at 1:1 with parent recompositions.

        // FIX: With `data class CartSummary`, only the 2 select clicks
        // cause recomposition (the content actually changes).
        composeTestRule.onNodeWithTag("cart_banner").assertRecompositions(exactly = 2)
    }

    // ============================================================
    // COMBINED INTERACTIONS
    // ============================================================

    @Test
    fun combinedInteractions_detectsAccumulatedIssues() {
        // Do multiple different interactions
        composeTestRule.onNodeWithTag("select_button").performClick()
        composeTestRule.onNodeWithTag("refresh_button").performClick()
        composeTestRule.onNodeWithTag("select_button").performClick()

        // Header recomposes on each select click (selectedCount changes)
        // but NOT on refresh (selectedCount stays the same, Compose skips).
        // 2 select clicks = 2 recompositions.
        composeTestRule.onNodeWithTag("product_header").assertRecompositions(exactly = 2)

        // Optimized header: first select flips false->true (1 recomp),
        // refresh is skipped, second select changes nothing for the boolean
        // (still true). So only 1 recomposition.
        composeTestRule.onNodeWithTag("optimized_header").assertRecompositions(exactly = 1)

        // TotalDisplay only recomposes on select changes (2 times)
        composeTestRule.onNodeWithTag("total_display").assertRecompositions(exactly = 2)

        // Footer stays stable due to strong skipping mode memoizing the lambda
        composeTestRule.onNodeWithTag("product_footer").assertStable()

        // ISSUE: CartBanner recomposes on ALL 3 interactions (2 selects + 1 refresh)
        // because each parent recomposition creates a new CartSummary instance.
        // FIX: With `data class CartSummary`, only the 2 selects cause
        // recomposition.
        composeTestRule.onNodeWithTag("cart_banner").assertRecompositions(exactly = 2)
    }

    // ============================================================
    // CONTRAST TESTS: Side-by-side comparison of issue vs fix
    // ============================================================

    @Test
    fun contrast_headerVsOptimizedHeader_onRepeatedSelections() {
        // 5 selections: selectedCount goes 0->1->2->3->4->5
        // Boolean hasSelection flips once: false->true
        repeat(5) {
            composeTestRule.onNodeWithTag("select_button").performClick()
        }

        // ISSUE (ProductHeader): at least 5 recompositions -- one per Int change
        composeTestRule.onNodeWithTag("product_header").assertRecompositions(atLeast = 5)

        // FIXED (OptimizedProductHeader): 1 recomposition -- the boolean flip
        composeTestRule.onNodeWithTag("optimized_header").assertRecompositions(exactly = 1)

        // This contrast clearly shows the 5:1 ratio of wasted recompositions
        // caused by using an overly broad parameter type.
    }

}
