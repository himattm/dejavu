package dejavu

import androidx.compose.runtime.Composer
import androidx.compose.runtime.InternalComposeTracingApi
import dejavu.internal.DejavuTracer

/**
 * Main entry point for enabling Dejavu's implicit recomposition tracking
 * on iOS.
 */
public actual object Dejavu {
    @kotlin.concurrent.Volatile
    internal var loggingEnabled: Boolean = false

    /**
     * Installs the CompositionTracer and starts tracking recompositions.
     */
    @OptIn(InternalComposeTracingApi::class)
    public fun enable() {
        loggingEnabled = false
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
