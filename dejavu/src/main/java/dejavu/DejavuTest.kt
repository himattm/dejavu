package dejavu

import dejavu.internal.DejavuTracer
import dejavu.internal.RecomposeTracker

internal object DejavuTest {
  fun reset() {
    RecomposeTracker.reset()
    DejavuTracer.reset()
  }
}
