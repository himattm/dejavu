package dejavu

import androidx.compose.runtime.Composer
import androidx.compose.runtime.InternalComposeTracingApi
import dejavu.internal.DejavuTracer

/**
 * Main entry point for enabling Dejavu's implicit recomposition tracking
 * on WasmJs.
 */
public actual object Dejavu {
    // No @Volatile needed: WasmJs is single-threaded (no multi-threaded memory model).
    internal var loggingEnabled: Boolean = false

    /**
     * Installs the CompositionTracer and starts tracking recompositions.
     *
     * @param logToConsole When true, recomposition events are printed via console.log
     */
    @OptIn(InternalComposeTracingApi::class)
    public fun enable(logToConsole: Boolean = false) {
        loggingEnabled = logToConsole
        DejavuTracer.enabled = true
        Composer.setTracer(DejavuTracer)
    }

    /**
     * Removes the tracer and clears all tracked recomposition data.
     */
    public actual fun disable() {
        DejavuTracer.enabled = false
        loggingEnabled = false
    }
}
