package dejavu

import androidx.compose.runtime.Composer
import androidx.compose.runtime.InternalComposeTracingApi
import dejavu.internal.DejavuTracer

/**
 * Main entry point for enabling Dejavu's implicit recomposition tracking
 * on Desktop (JVM).
 */
public actual object Dejavu {
    @kotlin.concurrent.Volatile
    internal var logToStdout: Boolean = false

    /**
     * Installs the CompositionTracer and starts tracking recompositions.
     *
     * @param logToStdout When true, recomposition events are printed to stdout
     */
    @OptIn(InternalComposeTracingApi::class)
    public fun enable(logToStdout: Boolean = false) {
        this.logToStdout = logToStdout
        DejavuTracer.enabled = true
        Composer.setTracer(DejavuTracer)
    }

    /**
     * Removes the tracer and clears all tracked recomposition data.
     */
    public actual fun disable() {
        DejavuTracer.enabled = false
        logToStdout = false
    }
}
