package demo.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dejavu.assertRecompositions
import dejavu.createRecompositionTrackingRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that validate the content of error messages produced by Dejavu's
 * recomposition assertions. These lock in the enriched error output so
 * regressions in diagnostic quality are caught.
 */
@RunWith(AndroidJUnit4::class)
class ErrorMessageValidationTest {

    @get:Rule
    val composeTestRule = createRecompositionTrackingRule<ProductActivity>()

    // ── Helpers ──────────────────────────────────────────────────

    private fun triggerThreeSelections() {
        repeat(3) {
            composeTestRule.onNodeWithTag("select_button").performClick()
        }
    }

    private fun catchAssertionError(block: () -> Unit): AssertionError {
        val error = try {
            block()
            null
        } catch (ae: AssertionError) {
            ae
        }
        assertThat(error).isNotNull()
        return error!!
    }

    // ── Test 1: Header + Expected/Actual ────────────────────────

    @Test
    fun errorMessage_containsHeaderAndExpectedActual() {
        triggerThreeSelections()

        val error = catchAssertionError {
            composeTestRule.onNodeWithTag("product_header")
                .assertRecompositions(exactly = 1)
        }

        assertThat(error.message).contains("Recomposition assertion failed for testTag='product_header'")
        assertThat(error.message).contains("Expected: exactly 1 recomposition(s)")
        assertThat(error.message).contains("Actual: 3 recomposition(s)")
    }

    // ── Test 2: Source Location ─────────────────────────────────

    @Test
    fun errorMessage_containsSourceLocation() {
        triggerThreeSelections()

        val error = catchAssertionError {
            composeTestRule.onNodeWithTag("product_header")
                .assertRecompositions(exactly = 1)
        }

        assertThat(error.message).contains("Composable: demo.app.ui.ProductHeader")
        assertThat(error.message).contains("ProductList.kt:")
    }

    // ── Test 3: All Tracked Composables ─────────────────────────

    @Test
    fun errorMessage_containsAllTrackedComposables() {
        triggerThreeSelections()

        val error = catchAssertionError {
            composeTestRule.onNodeWithTag("product_header")
                .assertRecompositions(exactly = 1)
        }

        assertThat(error.message).contains("All tracked composables:")
        assertThat(error.message).contains("ProductHeader")
        assertThat(error.message).contains("<-- FAILED")
        assertThat(error.message).contains("OptimizedProductHeader")
        assertThat(error.message).contains("TotalDisplay")
    }

    // ── Test 4: Recomposition Timeline ──────────────────────────

    @Test
    fun errorMessage_containsRecompositionTimeline() {
        triggerThreeSelections()

        val error = catchAssertionError {
            composeTestRule.onNodeWithTag("product_header")
                .assertRecompositions(exactly = 1)
        }

        assertThat(error.message).contains("Recomposition timeline:")
        assertThat(error.message).contains("#1 at +")
        assertThat(error.message).contains("#2 at +")
        assertThat(error.message).contains("#3 at +")
        assertThat(error.message).contains("param slots changed:")
    }

    // ── Test 5: Parent Info ─────────────────────────────────────

    @Test
    fun errorMessage_containsParentInfo() {
        triggerThreeSelections()

        val error = catchAssertionError {
            composeTestRule.onNodeWithTag("product_header")
                .assertRecompositions(exactly = 1)
        }

        assertThat(error.message).contains("parent: ProductListScreen")
    }

    // ── Test 6: Causality Info ──────────────────────────────────

    @Test
    fun errorMessage_containsCausalityInfo() {
        triggerThreeSelections()

        val error = catchAssertionError {
            composeTestRule.onNodeWithTag("product_header")
                .assertRecompositions(exactly = 1)
        }

        assertThat(error.message).contains("Possible cause:")
        assertThat(error.message).contains("Parameter/parent change detected")
    }

    // ── Test 7: Semantic Tree ────────────────────────────────────

    @Test
    fun errorMessage_containsSemanticTree() {
        triggerThreeSelections()

        val error = catchAssertionError {
            composeTestRule.onNodeWithTag("product_header")
                .assertRecompositions(exactly = 1)
        }

        // Single-node dump
        assertThat(error.message).contains("Node:")
        assertThat(error.message).contains("Tag: 'product_header'")

        // Full semantic tree
        assertThat(error.message).contains("Semantic tree:")
        assertThat(error.message).contains("<-- THIS NODE")
    }
}
