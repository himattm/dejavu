package demo.app

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object GroundTruthCounters {
    private val counts = ConcurrentHashMap<String, AtomicInteger>()

    fun increment(tag: String) {
        counts.getOrPut(tag) { AtomicInteger(0) }.incrementAndGet()
    }

    fun get(tag: String): Int = counts[tag]?.get() ?: 0

    fun reset() { counts.clear() }

    fun all(): Map<String, Int> = counts.mapValues { it.value.get() }
}
