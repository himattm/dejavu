package dejavu.internal

import java.util.concurrent.ConcurrentHashMap

internal object RecomposeTracker {
  private val causes = ConcurrentHashMap<String, RecomposeCause>()

  fun recordCause(
    label: String,
    cause: RecomposeCause? = null
  ) {
    if (cause != null) {
      causes[label] = cause
    }
  }

  fun getCause(label: String): RecomposeCause? {
    return causes[label]
  }

  fun reset() {
    causes.clear()
  }
}
