package dejavu.internal

import com.google.common.truth.Truth.assertThat
import dejavu.internal.ParameterChange
import dejavu.internal.ParamSnapshot
import dejavu.internal.ChangeType
import org.junit.Before
import org.junit.Test

class DejavuTracerUnitTest {

    @Before
    fun setUp() {
        DejavuTracer.reset()
    }

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

    // ── resolveUserComposable: dynamic resolution tests ────────────────

    @Test
    fun resolveUserComposable_returnsNull_forNullGroupName() {
        assertThat(DejavuTracer.resolveUserComposable(null, null)).isNull()
        assertThat(DejavuTracer.resolveUserComposable(null, "fallback")).isEqualTo("fallback")
    }

    @Test
    fun resolveUserComposable_returnsFallback_forRememberCalls() {
        assertThat(DejavuTracer.resolveUserComposable("remember", null)).isNull()
        assertThat(DejavuTracer.resolveUserComposable("remember(MyFile.kt:10)", null)).isNull()
    }

    @Test
    fun resolveUserComposable_returnsFallback_forFrameworkPrefixes() {
        assertThat(DejavuTracer.resolveUserComposable("androidx.compose.material3.Text", null)).isNull()
        assertThat(DejavuTracer.resolveUserComposable("kotlin.SomeFunction", null)).isNull()
        assertThat(DejavuTracer.resolveUserComposable("android.view.View", null)).isNull()
    }

    @Test
    fun resolveUserComposable_returnsFallback_forLowercaseNames() {
        assertThat(DejavuTracer.resolveUserComposable("content", null)).isNull()
        assertThat(DejavuTracer.resolveUserComposable("layout", null)).isNull()
    }

    @Test
    fun resolveUserComposable_returnsFallback_forEmptyName() {
        assertThat(DejavuTracer.resolveUserComposable("", null)).isNull()
    }

    @Test
    fun resolveUserComposable_resolvesFrameworkComposable_viaDynamicLookup() {
        // Simulate a framework composable being traced (parseInfo populates simpleNameIndex)
        DejavuTracer.parseInfo(1000, "androidx.compose.material3.Card (Card.kt:50)")

        // "Card" should resolve to the framework qualified name and be treated as framework
        assertThat(DejavuTracer.resolveUserComposable("Card", "fallback")).isEqualTo("fallback")
    }

    @Test
    fun resolveUserComposable_resolvesUserComposable_viaDynamicLookup() {
        // Simulate a user composable being traced
        DejavuTracer.parseInfo(1001, "com.myapp.ui.ProfileCard (Profile.kt:25)")

        // "ProfileCard" should resolve to the user qualified name and be returned
        assertThat(DejavuTracer.resolveUserComposable("ProfileCard", "fallback"))
            .isEqualTo("com.myapp.ui.ProfileCard")
    }

    @Test
    fun resolveUserComposable_returnsFallback_forUntracedGroupName() {
        // Names not in simpleNameIndex (Compose infrastructure like "Content", "Layout")
        // should conservatively be treated as framework
        assertThat(DejavuTracer.resolveUserComposable("Content", "fallback")).isEqualTo("fallback")
        assertThat(DejavuTracer.resolveUserComposable("ReusableComposeNode", "fallback")).isEqualTo("fallback")
    }

    @Test
    fun resolveUserComposable_prefersUserComposable_onNameCollision() {
        // Simulate both a framework and user composable with the same simple name
        DejavuTracer.parseInfo(1002, "androidx.compose.material3.Card (Card.kt:50)")
        DejavuTracer.parseInfo(1003, "com.myapp.ui.Card (CustomCard.kt:10)")

        // User composable should win — "Card" should resolve to the user's qualified name
        assertThat(DejavuTracer.resolveUserComposable("Card", "fallback"))
            .isEqualTo("com.myapp.ui.Card")
    }

    @Test
    fun resolveUserComposable_filtersAllMajorMaterialComposables_dynamically() {
        // Verify that common Material/Foundation composables are correctly filtered
        // without needing a hardcoded list — they're caught by simpleNameIndex resolution
        val frameworkComposables = mapOf(
            2000 to "androidx.compose.material3.Card (Card.kt:1)",
            2001 to "androidx.compose.material3.OutlinedTextField (OutlinedTextField.kt:1)",
            2002 to "androidx.compose.material3.FloatingActionButton (FloatingActionButton.kt:1)",
            2003 to "androidx.compose.material3.Checkbox (Checkbox.kt:1)",
            2004 to "androidx.compose.material3.NavigationBar (NavigationBar.kt:1)",
            2005 to "androidx.compose.material3.AlertDialog (AlertDialog.kt:1)",
            2006 to "androidx.compose.material3.Snackbar (Snackbar.kt:1)",
            2007 to "androidx.compose.material3.DropdownMenu (DropdownMenu.kt:1)",
            2008 to "androidx.compose.material3.Slider (Slider.kt:1)",
            2009 to "androidx.compose.material3.Switch (Switch.kt:1)",
            2010 to "androidx.compose.material3.TabRow (TabRow.kt:1)",
            2011 to "androidx.compose.material3.ModalBottomSheet (ModalBottomSheet.kt:1)",
            2012 to "androidx.compose.material3.SearchBar (SearchBar.kt:1)",
            2013 to "androidx.compose.material3.DatePicker (DatePicker.kt:1)",
            2014 to "androidx.compose.foundation.layout.FlowRow (FlowRow.kt:1)",
            2015 to "androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid (StaggeredGrid.kt:1)",
        )

        frameworkComposables.forEach { (key, info) ->
            DejavuTracer.parseInfo(key, info)
        }

        // Every one should be treated as framework (returns fallback)
        val simpleNames = listOf(
            "Card", "OutlinedTextField", "FloatingActionButton", "Checkbox",
            "NavigationBar", "AlertDialog", "Snackbar", "DropdownMenu",
            "Slider", "Switch", "TabRow", "ModalBottomSheet",
            "SearchBar", "DatePicker", "FlowRow", "LazyVerticalStaggeredGrid"
        )

        for (name in simpleNames) {
            assertThat(DejavuTracer.resolveUserComposable(name, "fallback"))
                .named("resolveUserComposable(\"$name\")")
                .isEqualTo("fallback")
        }
    }

}
