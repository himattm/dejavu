package dejavu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.InternalComposeTracingApi
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
 * Baseline capture is now handled internally by [DejavuTracer.resetCounts].
 */
internal fun resetRecompositionCounts() {
    DejavuTest.resetCounts()
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
 * Delegates to [DejavuTrackedContent] from the public API.
 * Existing tests use this name; kept as a thin wrapper.
 */
@Composable
internal fun DejavuTestContent(content: @Composable () -> Unit) {
    DejavuTrackedContent(content)
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
