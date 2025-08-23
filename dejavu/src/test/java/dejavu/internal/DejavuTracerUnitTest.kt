package dejavu.internal

import com.google.common.truth.Truth.assertThat
import dejavu.internal.ParameterChange
import dejavu.internal.ParamSnapshot
import dejavu.internal.ChangeType
import org.junit.Test

class DejavuTracerUnitTest {

    @Test
    fun parseInfo_parsesQualifiedNameAndSource() {
        val result = DejavuTracer.parseInfo(1, "com.example.MyComposable (MyFile.kt:42)")
        assertThat(result.qualifiedName).isEqualTo("com.example.MyComposable")
        assertThat(result.simpleName).isEqualTo("MyComposable")
        assertThat(result.sourceLocation).isEqualTo("MyFile.kt:42")
        assertThat(result.key).isEqualTo(1)
        assertThat(result.fullInfo).isEqualTo("com.example.MyComposable (MyFile.kt:42)")
    }

    @Test
    fun parseInfo_handlesNoSourceLocation() {
        val result = DejavuTracer.parseInfo(99, "com.example.NoSource")
        assertThat(result.qualifiedName).isEqualTo("com.example.NoSource")
        assertThat(result.simpleName).isEqualTo("NoSource")
        assertThat(result.sourceLocation).isEmpty()
        assertThat(result.key).isEqualTo(99)
        assertThat(result.fullInfo).isEqualTo("com.example.NoSource")
    }

    @Test
    fun isFrameworkComposable_filtersAndroidx() {
        assertThat(DejavuTracer.isFrameworkComposable("androidx.compose.material3.Text (Text.kt:100)")).isTrue()
        assertThat(DejavuTracer.isFrameworkComposable("androidx.compose.runtime.remember")).isTrue()
    }

    @Test
    fun isFrameworkComposable_filtersRememberCall() {
        assertThat(DejavuTracer.isFrameworkComposable("remember")).isTrue()
        assertThat(DejavuTracer.isFrameworkComposable("remember(MyFile.kt:10)")).isTrue()
    }

    @Test
    fun isFrameworkComposable_allowsUserRememberPrefix() {
        // User composables starting with "remember" but not exactly "remember" or "remember("
        // should NOT be filtered. E.g. "rememberMyState" is user code.
        assertThat(DejavuTracer.isFrameworkComposable("rememberMyState (MyFile.kt:5)")).isFalse()
        assertThat(DejavuTracer.isFrameworkComposable("rememberSaveable")).isFalse()
    }

    @Test
    fun isFrameworkComposable_filtersGetterAccessors() {
        assertThat(DejavuTracer.isFrameworkComposable("<get-isVisible>")).isTrue()
        assertThat(DejavuTracer.isFrameworkComposable("<get-currentState> (State.kt:10)")).isTrue()
    }

    @Test
    fun parameterChange_dataClass_roundTrip() {
        val change = ParameterChange(
            parameterName = "selected",
            oldValue = "false",
            newValue = "true",
            changeType = ChangeType.VALUE_CHANGED
        )
        assertThat(change.parameterName).isEqualTo("selected")
        assertThat(change.oldValue).isEqualTo("false")
        assertThat(change.newValue).isEqualTo("true")
        assertThat(change.changeType).isEqualTo(ChangeType.VALUE_CHANGED)
    }

    @Test
    fun paramSnapshot_dataClass_roundTrip() {
        val snapshot = ParamSnapshot(
            name = "count",
            valueHash = 42,
            valueString = "42"
        )
        assertThat(snapshot.name).isEqualTo("count")
        assertThat(snapshot.valueHash).isEqualTo(42)
        assertThat(snapshot.valueString).isEqualTo("42")
    }

    @Test
    fun changeType_hasAllExpectedValues() {
        val values = ChangeType.values()
        assertThat(values).hasLength(4)
        assertThat(values).asList().containsExactly(
            ChangeType.VALUE_CHANGED,
            ChangeType.REFERENCE_CHANGED,
            ChangeType.ADDED,
            ChangeType.REMOVED
        )
    }

}
