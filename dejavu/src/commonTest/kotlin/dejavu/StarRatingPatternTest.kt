package dejavu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Cross-platform port of Android StarRatingTest.
 * Star rating with Unicode characters. Validates per-star recomposition
 * tracking and stability when rating value does not change.
 */
@OptIn(ExperimentalTestApi::class)
class StarRatingPatternTest {

    @BeforeTest
    fun setUp() = enableDejavuForTest()

    @AfterTest
    fun tearDown() = disableDejavuForTest()

    @Test
    fun rating_allStarsRecomposeOnChange() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()

        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()

        onNodeWithTag("star_0").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_1").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_2").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_3").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_4").assertRecompositions(atLeast = 1)
    }

    @Test
    fun rating_displayRecomposesOnChange() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()

        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()

        onNodeWithTag("rating_display").assertRecompositions(atLeast = 1)
    }

    @Test
    fun rating_barRecomposesOnChange() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()

        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()

        onNodeWithTag("rating_bar").assertRecompositions(atLeast = 1)
    }

    @Test
    fun rating_sameValueNoRecomposition() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()

        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()

        onNodeWithTag("star_0").assertStable()
        onNodeWithTag("star_1").assertStable()
        onNodeWithTag("star_2").assertStable()
        onNodeWithTag("star_3").assertStable()
        onNodeWithTag("star_4").assertStable()
    }

    @Test
    fun rating_staticLabelStable() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()

        onNodeWithTag("set_rating_1_btn").performClick()
        waitForIdle()
        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()
        onNodeWithTag("set_rating_5_btn").performClick()
        waitForIdle()

        onNodeWithTag("static_rating_label").assertStable()
    }

    @Test
    fun rating_buttonsStable() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()

        // SetRatingButton is multi-instance (3 instances share counter).
        // After resetRecompositionCounts, only the click-triggered recomposition
        // should be counted. Non-clicked buttons recompose because parent passes
        // new onClick lambdas; assert they recompose at most once.
        onNodeWithTag("set_rating_1_btn").assertRecompositions(atMost = 1)
        onNodeWithTag("set_rating_5_btn").assertRecompositions(atMost = 1)
    }

    // ── Per-tag tracking regression tests (port of Android PerTagTrackingRegressionTest) ──
    //
    // Per-tag fingerprint tracking for multi-instance composables requires the
    // Android Choreographer frame loop to continuously rebuild tag mappings.
    // On non-Android platforms (JVM, iOS, WasmJs), per-tag tracking falls back
    // to function-level counting for multi-instance composables. The tests below
    // validate changed stars recompose; per-instance stability for unchanged stars
    // is only testable on Android (see PerTagTrackingRegressionTest in demo/androidTest).

    /**
     * Rating 0→3: stars 0-2 change (isFilled false→true). After reset, verify
     * that changed stars are detected as recomposed.
     */
    @Test
    fun multiInstance_changedInstancesDetected() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()
        resetRecompositionCounts()

        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()

        // Stars that changed should show recomposition
        onNodeWithTag("star_0").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_1").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_2").assertRecompositions(atLeast = 1)
    }

    /**
     * Rating 0→3, reset, then 3→5: stars 3-4 change from unfilled to filled.
     * Verifies changed stars are detected across a reset boundary.
     */
    @Test
    fun perTagDetection_afterReset_changedInstancesDetected() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()

        // Establish initial state: rating = 3
        onNodeWithTag("set_rating_3_btn").performClick()
        waitForIdle()
        resetRecompositionCounts()

        // Change rating 3→5: stars 3-4 become filled
        onNodeWithTag("set_rating_5_btn").performClick()
        waitForIdle()

        // Stars 3-4 changed — should show recomposition
        onNodeWithTag("star_3").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_4").assertRecompositions(atLeast = 1)
    }

    @Test
    fun rating_sequentialChanges() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()

        onNodeWithTag("set_rating_1_btn").performClick()
        waitForIdle()
        onNodeWithTag("set_rating_5_btn").performClick()
        waitForIdle()

        onNodeWithTag("star_0").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_1").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_2").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_3").assertRecompositions(atLeast = 1)
        onNodeWithTag("star_4").assertRecompositions(atLeast = 1)
    }

    @Test
    fun rating_noRecompositionWithoutInteraction() = runComposeUiTest {
        setContent { DejavuTestContent { RatingBarScreen() } }
        waitForIdle()

        onNodeWithTag("rating_bar_root").assertStable()
        onNodeWithTag("rating_bar").assertStable()
        // Star and SetRatingButton are multi-instance (share qualified-name counter).
        // Per-instance stability is tested via resetRecompositionCounts in
        // rating_sameValueNoRecomposition.
        onNodeWithTag("rating_display").assertStable()
        onNodeWithTag("static_rating_label").assertStable()
    }
}

// ── Composables ──────────────────────────────────────────────────

@Composable
private fun RatingBarScreen() {
    var rating by remember { mutableFloatStateOf(0f) }
    Column(Modifier.testTag("rating_bar_root")) {
        StaticRatingLabel()
        RatingBar(rating = rating, onRatingChange = { rating = it })
        RatingDisplay(rating = rating)
        SetRatingButton(label = "Set 1", tag = "set_rating_1_btn") { rating = 1f }
        SetRatingButton(label = "Set 3", tag = "set_rating_3_btn") { rating = 3f }
        SetRatingButton(label = "Set 5", tag = "set_rating_5_btn") { rating = 5f }
    }
}

@Composable
private fun StaticRatingLabel() {
    BasicText("Rate this item", Modifier.testTag("static_rating_label"))
}

@Composable
private fun RatingBar(rating: Float, onRatingChange: (Float) -> Unit) {
    Row(Modifier.testTag("rating_bar")) {
        for (i in 0 until 5) {
            Star(
                index = i,
                isFilled = i < rating,
                onClick = { onRatingChange((i + 1).toFloat()) },
            )
        }
    }
}

@Composable
private fun Star(index: Int, isFilled: Boolean, onClick: () -> Unit) {
    BasicText(
        text = if (isFilled) "\u2605" else "\u2606",
        modifier = Modifier.testTag("star_$index").clickable { onClick() },
    )
}

@Composable
private fun RatingDisplay(rating: Float) {
    BasicText("Rating: $rating / 5.0", Modifier.testTag("rating_display"))
}

@Composable
private fun SetRatingButton(label: String, tag: String, onClick: () -> Unit) {
    BasicText(label, Modifier.testTag(tag).clickable { onClick() })
}
