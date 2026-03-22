package dejavu

import dejavu.internal.DejavuTracer
import dejavu.internal.RecomposeTracker

internal object DejavuTest {
  /** Full clear of all state — use between independent tests. */
  fun reset() {
    RecomposeTracker.reset()
    DejavuTracer.reset()
  }

  /**
   * Clears recomposition counts but preserves composition history.
   * Use mid-test when the same composition is still alive.
   */
  fun resetCounts() {
    RecomposeTracker.reset()
    DejavuTracer.resetCounts()
  }
}
