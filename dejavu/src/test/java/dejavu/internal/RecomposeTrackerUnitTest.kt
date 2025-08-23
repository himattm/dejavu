package dejavu.internal

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class RecomposeTrackerUnitTest {

    @After
    fun tearDown() {
        RecomposeTracker.reset()
    }

    @Test
    fun recordCause_andGetCause_returnsRecorded() {
        val cause = RecomposeCause(stateChanges = 2, types = listOf("Int"), isParameterDriven = true)
        RecomposeTracker.recordCause("com.example.MyComposable", cause)

        val retrieved = RecomposeTracker.getCause("com.example.MyComposable")
        assertThat(retrieved).isEqualTo(cause)
    }

    @Test
    fun getCause_unrecorded_returnsNull() {
        val retrieved = RecomposeTracker.getCause("com.example.NeverRecorded")
        assertThat(retrieved).isNull()
    }

    @Test
    fun reset_clearsCauses() {
        val cause = RecomposeCause(stateChanges = 1)
        RecomposeTracker.recordCause("com.example.ToClear", cause)
        assertThat(RecomposeTracker.getCause("com.example.ToClear")).isNotNull()

        RecomposeTracker.reset()

        assertThat(RecomposeTracker.getCause("com.example.ToClear")).isNull()
    }

    @Test
    fun recordCause_nullCause_doesNotOverwrite() {
        val original = RecomposeCause(stateChanges = 3, types = listOf("String"))
        RecomposeTracker.recordCause("com.example.Keep", original)

        // Recording a null cause should NOT overwrite the existing one
        RecomposeTracker.recordCause("com.example.Keep", null)

        assertThat(RecomposeTracker.getCause("com.example.Keep")).isEqualTo(original)
    }
}
