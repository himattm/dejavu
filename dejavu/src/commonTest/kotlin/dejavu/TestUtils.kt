package dejavu

import androidx.compose.runtime.Composer
import androidx.compose.runtime.InternalComposeTracingApi
import dejavu.internal.DejavuTracer

/**
 * Enables the Dejavu tracer for testing without platform-specific dependencies.
 * Call before each test, paired with [disableDejavuForTest] after.
 */
@OptIn(InternalComposeTracingApi::class)
internal fun enableDejavuForTest() {
    DejavuTracer.enabled = true
    Composer.setTracer(DejavuTracer)
    DejavuTest.reset()
}

/**
 * Disables the Dejavu tracer after testing.
 */
internal fun disableDejavuForTest() {
    DejavuTracer.enabled = false
}
