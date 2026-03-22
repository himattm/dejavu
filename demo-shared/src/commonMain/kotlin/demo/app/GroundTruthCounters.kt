package demo.app

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

object GroundTruthCounters : SynchronizedObject() {
    private val counts = mutableMapOf<String, Int>()

    fun increment(tag: String): Unit = synchronized(this) {
        counts[tag] = (counts[tag] ?: 0) + 1
    }

    fun get(tag: String): Int = synchronized(this) { counts[tag] ?: 0 }

    fun reset(): Unit = synchronized(this) { counts.clear() }

    fun all(): Map<String, Int> = synchronized(this) { counts.toMap() }
}
