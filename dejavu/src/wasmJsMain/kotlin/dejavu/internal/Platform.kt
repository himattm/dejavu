package dejavu.internal

import androidx.compose.runtime.tooling.CompositionData
import dejavu.Dejavu

internal actual fun currentTimeMillis(): Long =
    dateNow().toLong()

private fun dateNow(): Double = js("Date.now()")

internal actual fun platformLog(tag: String, message: String) {
    console.log("$tag: $message".toJsString())
}

internal actual fun platformWarnLog(tag: String, message: String) {
    console.warn("WARN $tag: $message".toJsString())
}

internal actual fun getPendingCause(): RecomposeCause? = null

internal actual fun isLoggingEnabled(): Boolean = Dejavu.loggingEnabled

internal actual fun currentCompositionsSnapshot(): Set<CompositionData> =
    DejavuTracer.inspectionTables.toSet()

internal actual fun platformBuildTagMapping(compositionData: Set<CompositionData>) {
    CommonTagMapping.buildTagMapping(compositionData)
}

internal actual class PlatformThreadLocal<T> actual constructor(private val initial: () -> T) {
    private var value: T = initial()

    actual fun get(): T = value
}

private external object console {
    fun log(message: JsString)
    fun warn(message: JsString)
}
