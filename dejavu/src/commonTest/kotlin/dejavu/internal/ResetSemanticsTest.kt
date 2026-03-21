package dejavu.internal

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResetSemanticsTest {

    @BeforeTest
    fun setUp() {
        DejavuTracer.enabled = true
        DejavuTracer.reset()
    }

    @Test
    fun reset_clearsCompositionHistoryAndCounts() {
        // Simulate two compositions of the same key (initial + recomposition)
        DejavuTracer.traceEventStart(100, 0, 0, "com.example.Foo (Foo.kt:1)")
        DejavuTracer.traceEventEnd()
        DejavuTracer.traceEventStart(100, 1, 0, "com.example.Foo (Foo.kt:1)")
        DejavuTracer.traceEventEnd()

        assertEquals(1, DejavuTracer.getRecompositionCount("com.example.Foo"))
        assertTrue(DejavuTracer.getAllTracedComposables().isNotEmpty())

        DejavuTracer.reset()

        assertEquals(0, DejavuTracer.getRecompositionCount("com.example.Foo"))
        assertTrue(DejavuTracer.getAllTracedComposables().isEmpty())
    }

    @Test
    fun resetCounts_clearsCountsButPreservesHistory() {
        // Simulate two compositions of the same key (initial + recomposition)
        DejavuTracer.traceEventStart(200, 0, 0, "com.example.Bar (Bar.kt:1)")
        DejavuTracer.traceEventEnd()
        DejavuTracer.traceEventStart(200, 1, 0, "com.example.Bar (Bar.kt:1)")
        DejavuTracer.traceEventEnd()

        assertEquals(1, DejavuTracer.getRecompositionCount("com.example.Bar"))

        DejavuTracer.resetCounts()

        // Counts are cleared
        assertEquals(0, DejavuTracer.getRecompositionCount("com.example.Bar"))
        // But composition history (keyToInfo, compositionCounts) is preserved
        assertTrue(DejavuTracer.getAllTracedComposables().isNotEmpty())
    }

    @Test
    fun resetCounts_nextCompositionCountsAsRecomposition() {
        // Initial composition
        DejavuTracer.traceEventStart(300, 0, 0, "com.example.Baz (Baz.kt:1)")
        DejavuTracer.traceEventEnd()

        DejavuTracer.resetCounts()

        // After resetCounts, compositionCounts still has key 300 with count 1.
        // The next traceEventStart for key 300 increments to 2 (totalCount > 1),
        // so it IS counted as a recomposition.
        DejavuTracer.traceEventStart(300, 1, 0, "com.example.Baz (Baz.kt:1)")
        DejavuTracer.traceEventEnd()

        assertEquals(1, DejavuTracer.getRecompositionCount("com.example.Baz"))
    }
}
