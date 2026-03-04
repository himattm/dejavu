package dejavu.internal

import androidx.compose.runtime.tooling.CompositionData
import dejavu.Dejavu
import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.timeIntervalSince1970

internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

internal actual fun platformLog(tag: String, message: String) {
    NSLog("$tag: $message")
}

internal actual fun platformWarnLog(tag: String, message: String) {
    NSLog("WARN $tag: $message")
}

internal actual fun getPendingCause(): RecomposeCause? = null

internal actual fun isLoggingEnabled(): Boolean = Dejavu.loggingEnabled

internal actual fun currentCompositionsSnapshot(): Set<CompositionData> = emptySet()

internal actual fun platformBuildTagMapping(compositionData: Set<CompositionData>) {
    // No-op on iOS: tag mapping requires Android's Group tree walker
}

internal actual class PlatformThreadLocal<T> actual constructor(private val initial: () -> T) {
    private var value: T = initial()

    actual fun get(): T = value
}
