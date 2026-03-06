package dejavu.internal

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecomposeTrackerUnitTest {

    @AfterTest
    fun tearDown() {
        RecomposeTracker.reset()
    }

    @Test
    fun recordCause_andGetCause_returnsRecorded() {
        val cause = RecomposeCause(stateChanges = 2, types = listOf("Int"), isParameterDriven = true)
        RecomposeTracker.recordCause("com.example.MyComposable", cause)

        val retrieved = RecomposeTracker.getCause("com.example.MyComposable")
        assertEquals(cause, retrieved)
    }

    @Test
    fun getCause_unrecorded_returnsNull() {
        val retrieved = RecomposeTracker.getCause("com.example.NeverRecorded")
        assertNull(retrieved)
    }

    @Test
    fun reset_clearsCauses() {
        val cause = RecomposeCause(stateChanges = 1)
        RecomposeTracker.recordCause("com.example.ToClear", cause)
        assertNotNull(RecomposeTracker.getCause("com.example.ToClear"))

        RecomposeTracker.reset()

        assertNull(RecomposeTracker.getCause("com.example.ToClear"))
    }

    @Test
    fun recordCause_nullCause_doesNotOverwrite() {
        val original = RecomposeCause(stateChanges = 3, types = listOf("String"))
        RecomposeTracker.recordCause("com.example.Keep", original)

        // Recording a null cause should NOT overwrite the existing one
        RecomposeTracker.recordCause("com.example.Keep", null)

        assertEquals(original, RecomposeTracker.getCause("com.example.Keep"))
    }
}
