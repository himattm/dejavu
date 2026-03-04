package dejavu.internal

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal object RecomposeTracker {
  private val lock = SynchronizedObject()
  private val causes = mutableMapOf<String, RecomposeCause>()

  fun recordCause(
    label: String,
    cause: RecomposeCause? = null
  ) {
    synchronized(lock) {
      if (cause != null) {
        causes[label] = cause
      }
    }
  }

  fun getCause(label: String): RecomposeCause? {
    return synchronized(lock) {
      causes[label]
    }
  }

  fun reset() {
    synchronized(lock) {
      causes.clear()
    }
  }
}
