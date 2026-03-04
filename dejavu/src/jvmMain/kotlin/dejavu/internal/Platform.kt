package dejavu.internal

import androidx.compose.runtime.tooling.CompositionData
import dejavu.Dejavu

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun platformLog(tag: String, message: String) {
    println("$tag: $message")
}

internal actual fun platformWarnLog(tag: String, message: String) {
    System.err.println("WARN $tag: $message")
}

internal actual fun getPendingCause(): RecomposeCause? = null

internal actual fun isLoggingEnabled(): Boolean = Dejavu.logToStdout

internal actual fun currentCompositionsSnapshot(): Set<CompositionData> = emptySet()

internal actual fun platformBuildTagMapping(compositionData: Set<CompositionData>) {
    // No-op on Desktop: tag mapping requires Android's Group tree walker
}

internal actual class PlatformThreadLocal<T> actual constructor(private val initial: () -> T) {
    private val tl = object : ThreadLocal<T>() {
        override fun initialValue(): T = initial()
    }

    actual fun get(): T = tl.get()!!
}
