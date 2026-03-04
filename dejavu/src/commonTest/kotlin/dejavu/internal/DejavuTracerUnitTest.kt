package dejavu.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DejavuTracerUnitTest {

    @Test
    fun parseInfo_parsesQualifiedNameAndSource() {
        val result = DejavuTracer.parseInfo(1, "com.example.MyComposable (MyFile.kt:42)")
        assertEquals("com.example.MyComposable", result.qualifiedName)
        assertEquals("MyComposable", result.simpleName)
        assertEquals("MyFile.kt:42", result.sourceLocation)
        assertEquals(1, result.key)
        assertEquals("com.example.MyComposable (MyFile.kt:42)", result.fullInfo)
    }

    @Test
    fun parseInfo_handlesNoSourceLocation() {
        val result = DejavuTracer.parseInfo(99, "com.example.NoSource")
        assertEquals("com.example.NoSource", result.qualifiedName)
        assertEquals("NoSource", result.simpleName)
        assertEquals("", result.sourceLocation)
        assertEquals(99, result.key)
        assertEquals("com.example.NoSource", result.fullInfo)
    }

    @Test
    fun isFrameworkComposable_filtersAndroidx() {
        assertTrue(DejavuTracer.isFrameworkComposable("androidx.compose.material3.Text (Text.kt:100)"))
        assertTrue(DejavuTracer.isFrameworkComposable("androidx.compose.runtime.remember"))
    }

    @Test
    fun isFrameworkComposable_filtersRememberCall() {
        assertTrue(DejavuTracer.isFrameworkComposable("remember"))
        assertTrue(DejavuTracer.isFrameworkComposable("remember(MyFile.kt:10)"))
    }

    @Test
    fun isFrameworkComposable_allowsUserRememberPrefix() {
        assertFalse(DejavuTracer.isFrameworkComposable("rememberMyState (MyFile.kt:5)"))
        assertFalse(DejavuTracer.isFrameworkComposable("rememberSaveable"))
    }

    @Test
    fun isFrameworkComposable_filtersGetterAccessors() {
        assertTrue(DejavuTracer.isFrameworkComposable("<get-isVisible>"))
        assertTrue(DejavuTracer.isFrameworkComposable("<get-currentState> (State.kt:10)"))
    }

    @Test
    fun parameterChange_dataClass_roundTrip() {
        val change = ParameterChange(
            parameterName = "selected",
            oldValue = "false",
            newValue = "true",
            changeType = ChangeType.VALUE_CHANGED
        )
        assertEquals("selected", change.parameterName)
        assertEquals("false", change.oldValue)
        assertEquals("true", change.newValue)
        assertEquals(ChangeType.VALUE_CHANGED, change.changeType)
    }

    @Test
    fun paramSnapshot_dataClass_roundTrip() {
        val snapshot = ParamSnapshot(
            name = "count",
            valueHash = 42,
            valueString = "42"
        )
        assertEquals("count", snapshot.name)
        assertEquals(42, snapshot.valueHash)
        assertEquals("42", snapshot.valueString)
    }

    @Test
    fun changeType_hasAllExpectedValues() {
        val values = ChangeType.entries
        assertEquals(4, values.size)
        assertTrue(values.containsAll(listOf(
            ChangeType.VALUE_CHANGED,
            ChangeType.REFERENCE_CHANGED,
            ChangeType.ADDED,
            ChangeType.REMOVED
        )))
    }
}
