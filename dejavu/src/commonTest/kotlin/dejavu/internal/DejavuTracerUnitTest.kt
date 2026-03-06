package dejavu.internal

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DejavuTracerUnitTest {

    @BeforeTest
    fun setUp() {
        DejavuTracer.reset()
    }

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

    // ── resolveUserComposable: dynamic resolution tests ────────────────

    @Test
    fun resolveUserComposable_returnsNull_forNullGroupName() {
        assertNull(DejavuTracer.resolveUserComposable(null, null))
        assertEquals("fallback", DejavuTracer.resolveUserComposable(null, "fallback"))
    }

    @Test
    fun resolveUserComposable_returnsFallback_forRememberCalls() {
        assertNull(DejavuTracer.resolveUserComposable("remember", null))
        assertNull(DejavuTracer.resolveUserComposable("remember(MyFile.kt:10)", null))
    }

    @Test
    fun resolveUserComposable_returnsFallback_forFrameworkPrefixes() {
        assertNull(DejavuTracer.resolveUserComposable("androidx.compose.material3.Text", null))
        assertNull(DejavuTracer.resolveUserComposable("kotlin.SomeFunction", null))
        assertNull(DejavuTracer.resolveUserComposable("android.view.View", null))
    }

    @Test
    fun resolveUserComposable_returnsFallback_forLowercaseNames() {
        assertNull(DejavuTracer.resolveUserComposable("content", null))
        assertNull(DejavuTracer.resolveUserComposable("layout", null))
    }

    @Test
    fun resolveUserComposable_returnsFallback_forEmptyName() {
        assertNull(DejavuTracer.resolveUserComposable("", null))
    }

    @Test
    fun resolveUserComposable_resolvesFrameworkComposable_viaDynamicLookup() {
        DejavuTracer.parseInfo(1000, "androidx.compose.material3.Card (Card.kt:50)")
        assertEquals("fallback", DejavuTracer.resolveUserComposable("Card", "fallback"))
    }

    @Test
    fun resolveUserComposable_resolvesUserComposable_viaDynamicLookup() {
        DejavuTracer.parseInfo(1001, "com.myapp.ui.ProfileCard (Profile.kt:25)")
        assertEquals("com.myapp.ui.ProfileCard", DejavuTracer.resolveUserComposable("ProfileCard", "fallback"))
    }

    @Test
    fun resolveUserComposable_returnsFallback_forUntracedGroupName() {
        assertEquals("fallback", DejavuTracer.resolveUserComposable("Content", "fallback"))
        assertEquals("fallback", DejavuTracer.resolveUserComposable("ReusableComposeNode", "fallback"))
    }

    @Test
    fun resolveUserComposable_prefersUserComposable_onNameCollision() {
        DejavuTracer.parseInfo(1002, "androidx.compose.material3.Card (Card.kt:50)")
        DejavuTracer.parseInfo(1003, "com.myapp.ui.Card (CustomCard.kt:10)")
        assertEquals("com.myapp.ui.Card", DejavuTracer.resolveUserComposable("Card", "fallback"))
    }

    @Test
    fun resolveUserComposable_filtersAllMajorMaterialComposables_dynamically() {
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
        frameworkComposables.forEach { (key, info) -> DejavuTracer.parseInfo(key, info) }

        val simpleNames = listOf(
            "Card", "OutlinedTextField", "FloatingActionButton", "Checkbox",
            "NavigationBar", "AlertDialog", "Snackbar", "DropdownMenu",
            "Slider", "Switch", "TabRow", "ModalBottomSheet",
            "SearchBar", "DatePicker", "FlowRow", "LazyVerticalStaggeredGrid"
        )
        for (name in simpleNames) {
            assertEquals("fallback", DejavuTracer.resolveUserComposable(name, "fallback"),
                "Expected '$name' to be treated as framework composable")
        }
    }
}
