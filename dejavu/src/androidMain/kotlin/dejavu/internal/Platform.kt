package dejavu.internal

import android.util.Log
import androidx.compose.runtime.tooling.CompositionData

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun platformLog(tag: String, message: String) {
    Log.d(tag, message)
}

internal actual fun platformWarnLog(tag: String, message: String) {
    Log.w(tag, message)
}

internal actual fun getPendingCause(): RecomposeCause? = Runtime.getPendingCause()

internal actual fun isLoggingEnabled(): Boolean = Runtime.isLoggingEnabled

internal actual fun currentCompositionsSnapshot(): Set<CompositionData> =
    Runtime.currentCompositionsSnapshot()

internal actual fun platformBuildTagMapping(compositionData: Set<CompositionData>) {
    TagMapping.buildTagMapping(compositionData)
}

internal actual class PlatformThreadLocal<T> actual constructor(private val initial: () -> T) {
    private val tl = object : ThreadLocal<T>() {
        override fun initialValue(): T = initial()
    }

    actual fun get(): T = tl.get()!!
}

internal actual fun onComposableTraced(qualifiedName: String) {
    Runtime.observerDelegate.bindPendingScope(qualifiedName)
}

internal actual fun describeInvalidationCauses(qualifiedName: String): String? =
    Runtime.observerDelegate.describeInvalidationCauses(qualifiedName)

internal actual fun describeStateDependencies(qualifiedName: String): String? =
    Runtime.observerDelegate.describeStateDependencies(qualifiedName)

internal actual fun isObserverAvailable(): Boolean =
    Runtime.observerDelegate.isAvailable

internal actual fun resetObserver() {
    Runtime.observerDelegate.reset()
}
