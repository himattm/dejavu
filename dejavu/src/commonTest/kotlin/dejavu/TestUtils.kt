package dejavu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import dejavu.internal.DejavuTracer

/**
 * Shared inspection tables set populated by the Compose runtime when
 * [LocalInspectionTables] is provided. Used by non-Android platforms
 * to access [CompositionData] for tag mapping.
 */
internal val testInspectionTables = mutableSetOf<CompositionData>()

/**
 * Enables the Dejavu tracer for testing without platform-specific dependencies.
 * Call before each test, paired with [disableDejavuForTest] after.
 */
@OptIn(InternalComposeTracingApi::class)
internal fun enableDejavuForTest() {
    isDebugInspectorInfoEnabled = true
    DejavuTracer.enabled = true
    Composer.setTracer(DejavuTracer)
    testInspectionTables.clear()
    DejavuTracer.inspectionTables.clear()
    DejavuTest.reset()
}

/**
 * Disables the Dejavu tracer after testing.
 */
internal fun disableDejavuForTest() {
    DejavuTracer.enabled = false
    isDebugInspectorInfoEnabled = false
    testInspectionTables.clear()
    DejavuTracer.inspectionTables.clear()
}

/**
 * Wraps test content with [LocalInspectionTables] so that the Compose runtime
 * populates [CompositionData] for tag mapping on all platforms.
 *
 * Uses [SubcomposeLayout] to create a sub-composition. The sub-composition's
 * `startRoot()` reads [LocalInspectionTables] from its parent context (this
 * composition), finds our provided set, and adds its [CompositionData] to it.
 * Without SubcomposeLayout, the root composition's `startRoot()` reads from the
 * Recomposer (which has no LocalInspectionTables) and never populates the set.
 *
 * Usage:
 * ```
 * setContent {
 *     DejavuTestContent {
 *         MyComposable()
 *     }
 * }
 * ```
 */
@Composable
internal fun DejavuTestContent(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInspectionTables provides testInspectionTables) {
        SubcomposeLayout { constraints ->
            val placeables = subcompose(Unit) { content() }
                .map { it.measure(constraints) }
            layout(
                placeables.maxOfOrNull { it.width } ?: 0,
                placeables.maxOfOrNull { it.height } ?: 0
            ) {
                placeables.forEach { it.place(0, 0) }
            }
        }
    }
}

/**
 * Refreshes tag mapping from the current composition data.
 * Call after [waitForIdle] and before making per-tag assertions.
 */
internal fun refreshTagMapping() {
    // Sync test inspection tables into the tracer
    DejavuTracer.inspectionTables.clear()
    DejavuTracer.inspectionTables.addAll(testInspectionTables)
    // Build tag mapping from the composition data
    DejavuTracer.buildTagMapping(testInspectionTables)
}
