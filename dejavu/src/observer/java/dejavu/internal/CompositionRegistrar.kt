package dejavu.internal

import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.CompositionRegistrationObserver
import androidx.compose.runtime.tooling.ObservableComposition
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Automatically installs [DejavuCompositionObserver] on every composition
 * registered with the Recomposer.
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
internal object CompositionRegistrar : CompositionRegistrationObserver {

    private val handles: MutableMap<ObservableComposition, CompositionObserverHandle> =
        Collections.synchronizedMap(IdentityHashMap())

    override fun onCompositionRegistered(composition: ObservableComposition) {
        if (handles.containsKey(composition)) return
        handles[composition] = composition.setObserver(DejavuCompositionObserver)
    }

    override fun onCompositionUnregistered(composition: ObservableComposition) {
        handles.remove(composition)?.dispose()
    }

    fun reset() {
        synchronized(handles) {
            handles.values.toList().also { handles.clear() }
        }.forEach { it.dispose() }
    }
}
