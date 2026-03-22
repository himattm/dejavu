package dejavu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import dejavu.internal.DejavuTracer
import kotlinx.atomicfu.locks.synchronized

/**
 * Alias for [DejavuTracer.inspectionTables]. Provided to [LocalInspectionTables]
 * so the Compose runtime populates the tracer's tables directly, enabling
 * [assertRecompositions]/[assertStable] to resolve testTag → function mappings
 * without a manual sync step.
 */
internal val testInspectionTables get() = DejavuTracer.inspectionTables

/**
 * Enables the Dejavu tracer for testing without platform-specific dependencies.
 * Call before each test, paired with [disableDejavuForTest] after.
 */
@OptIn(InternalComposeTracingApi::class)
internal fun enableDejavuForTest() {
    isDebugInspectorInfoEnabled = true
    DejavuTracer.enabled = true
    Composer.setTracer(DejavuTracer)
    synchronized(DejavuTracer.inspectionTablesLock) { DejavuTracer.inspectionTables.clear() }
    DejavuTest.reset()
}

/**
 * Clears recomposition counts but preserves composition history.
 * Use mid-test when a live composition is still running.
 *
 * Also captures fingerprint baselines from the current composition state
 * so that per-tag tracking can detect post-reset changes deterministically.
 */
internal fun resetRecompositionCounts() {
    DejavuTest.resetCounts()
    // Capture fingerprint baselines from current composition state,
    // then clear counts so only post-reset changes are reported.
    val snapshots = synchronized(DejavuTracer.inspectionTablesLock) {
        DejavuTracer.inspectionTables.toSet()
    }
    if (snapshots.isNotEmpty()) {
        DejavuTracer.buildTagMappingFromFrameLoop(snapshots)
        DejavuTracer.clearCountsPreservingBaselines()
    }
}

/**
 * Disables the Dejavu tracer after testing.
 */
internal fun disableDejavuForTest() {
    DejavuTracer.enabled = false
    isDebugInspectorInfoEnabled = false
    synchronized(DejavuTracer.inspectionTablesLock) { DejavuTracer.inspectionTables.clear() }
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
 * Provides [DejavuTracer.inspectionTables] directly so that the assertion API
 * ([assertRecompositions]/[assertStable]) can resolve testTag → function mappings
 * via [DejavuTracer.getCompositionSnapshots] without any manual sync.
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
    CompositionLocalProvider(LocalInspectionTables provides DejavuTracer.inspectionTables) {
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
    val snapshot = synchronized(DejavuTracer.inspectionTablesLock) {
        DejavuTracer.inspectionTables.toSet()
    }
    DejavuTracer.buildTagMapping(snapshot)
}

/** True only on WasmJs targets — used to skip tests with Wasm test runner limitations. */
internal expect val isWasmJs: Boolean

/** True only on iOS/Native targets — used to skip tests with upstream Compose runtime bugs. */
internal expect val isIos: Boolean
