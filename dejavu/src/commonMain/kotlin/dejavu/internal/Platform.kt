package dejavu.internal

import androidx.compose.runtime.tooling.CompositionData

/** Returns the current wall-clock time in milliseconds. */
internal expect fun currentTimeMillis(): Long

/** Platform-specific debug log (e.g. android.util.Log.d). */
internal expect fun platformLog(tag: String, message: String)

/** Platform-specific warning log (e.g. android.util.Log.w). */
internal expect fun platformWarnLog(tag: String, message: String)

/** Retrieves the pending [RecomposeCause] from the platform runtime, if any. */
internal expect fun getPendingCause(): RecomposeCause?

/** Whether platform-level diagnostic logging is enabled. */
internal expect fun isLoggingEnabled(): Boolean

/** Returns the current set of live [CompositionData] snapshots from the platform runtime. */
internal expect fun currentCompositionsSnapshot(): Set<CompositionData>

/**
 * Platform-specific tag-mapping pass over composition data.
 *
 * On Android this walks the Group tree (via `asTree()`) to map
 * `Modifier.testTag` values to enclosing user composable function names.
 * On other platforms this is a no-op.
 */
internal expect fun platformBuildTagMapping(compositionData: Set<CompositionData>)

/**
 * Thread-local storage abstraction.
 *
 * On JVM/Android this delegates to [java.lang.ThreadLocal].
 * Other platforms provide their own implementation.
 */
internal expect class PlatformThreadLocal<T>(initial: () -> T) {
    fun get(): T
}
