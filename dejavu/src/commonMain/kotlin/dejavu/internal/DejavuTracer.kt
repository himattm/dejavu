package dejavu.internal

import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.tooling.CompositionData
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

@OptIn(InternalComposeTracingApi::class)
internal object DejavuTracer : CompositionTracer {
    private const val TAG = "DejavuTracer"

    @kotlin.concurrent.Volatile
    var enabled = false

    // ── Shared state (internal so platform tag-mapping code can access) ──

    /** Count traceEventStart calls per composable key (int).
     *  First call is initial composition, subsequent are recompositions. */
    internal val compositionCounts = mutableMapOf<Int, Int>()
    internal val compositionCountsLock = SynchronizedObject()

    /** Map from key (int) to parsed info: functionName, fullInfo, sourceLocation. */
    internal val keyToInfo = mutableMapOf<Int, TracedComposable>()
    internal val keyToInfoLock = SynchronizedObject()

    /** Reverse index: simple name -> traced composables for O(1) qualified-name lookup. */
    internal val simpleNameIndex = mutableMapOf<String, MutableList<TracedComposable>>()
    internal val simpleNameIndexLock = SynchronizedObject()

    /** Map from qualified function name to recomposition count (excludes initial composition).
     *  This is the primary query API; query methods support simple name fallback. */
    internal val recompositionCounts = mutableMapOf<String, Int>()
    internal val recompositionCountsLock = SynchronizedObject()

    /** Timestamped recomposition events per qualified function name. */
    internal val recompositionEvents = mutableMapOf<String, MutableList<RecompositionEvent>>()
    internal val recompositionEventsLock = SynchronizedObject()

    /** Per-tag recomposition counting via Group tree parameter change detection. */
    internal val perTagRecompCounts = mutableMapOf<String, Int>()
    internal val tagParamFingerprints = mutableMapOf<String, Int>()
    internal val perTagRecompEvents = mutableMapOf<String, MutableList<RecompositionEvent>>()
    internal val perTagLock = SynchronizedObject()

    /** Parameter snapshots for change tracking (replaces raw fingerprint comparison). */
    internal val tagParamSnapshots = mutableMapOf<String, List<ParamSnapshot>>()
    internal val tagParameterChanges = mutableMapOf<String, MutableList<List<ParameterChange>>>()
    internal val tagParamLock = SynchronizedObject()

    /** Tags seen during the most recent buildTagMapping pass; used to detect stale entries. */
    internal val lastSeenTags = mutableSetOf<String>()
    internal val lastSeenTagsLock = SynchronizedObject()

    /** Stable per-instance identifier (Group.identity / Anchor) for each tag. */
    internal val tagToIdentity = mutableMapOf<String, Any>()
    internal val tagToIdentityLock = SynchronizedObject()

    /** Cache of testTag -> composable function name. */
    internal val testTagToFunction = mutableMapOf<String, String>()
    internal val testTagToFunctionLock = SynchronizedObject()

    /** Track parent-child causality: stack of qualified names for the current composition. */
    private val composableStack = PlatformThreadLocal { ArrayDeque<String>() }

    internal data class TracedComposable(
        val key: Int,
        val simpleName: String,      // e.g., "CounterValue"
        val qualifiedName: String,   // e.g., "demo.app.ui.CounterValue"
        val sourceLocation: String,  // e.g., "Counter.kt:29"
        val fullInfo: String         // raw info string
    )

    internal data class RecompositionEvent(
        val timestampMs: Long,
        val dirty1: Int,
        val qualifiedName: String,
        val parentName: String? = null
    )

    // ── Framework filtering ─────────────────────────────────────────

    /** Framework packages/files to skip when walking the Group tree. */
    internal val frameworkPrefixes = setOf("androidx.", "kotlin.", "android.")

    /** Compose Material/Foundation composable names that appear as Group names in the composition tree.
     *  User composables with these names should use their full qualified name for disambiguation. */
    internal val frameworkGroupNames = setOf(
        "ReusableComposeNode", "ComposeNode", "Layout", "ReusableContent",
        "ProvideCommonCompositionLocals", "Content", "CompositionLocalProvider",
        "Providers", "MaterialTheme", "Surface", "ProvideTextStyle",
        "BasicText", "Box", "Column", "Row", "Text", "Button",
        "Ripple", "Canvas", "Image", "Icon", "Scaffold",
        "LazyColumn", "LazyRow", "LazyList",
        "SubcomposeLayout", "SubcomposeLayoutState", "BoxWithConstraints",
        "LazyLayout", "LazyLayoutScrollScope",
        "LazySaveableStateHolderProvider", "LazyLayoutPrefetchState",
        "LazyLayoutItemContentFactory", "SingleRowTopAppBar",
        "TopAppBarLayout",
        "Spacer", "LazyVerticalGrid", "LazyHorizontalGrid",
        "AnimatedVisibility", "AnimatedContent", "Crossfade",
        "Dialog", "Popup", "TextField", "BasicTextField",
        "HorizontalPager", "VerticalPager",
    )

    // ── CompositionTracer implementation ─────────────────────────────

    override fun isTraceInProgress(): Boolean = enabled

    override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
        // Parse and cache info
        val traced = synchronized(keyToInfoLock) {
            keyToInfo.getOrPut(key) { parseInfo(key, info) }
        }

        // Push onto the composable stack BEFORE the framework check so that
        // traceEventEnd (which fires for every composable) can always pop.
        val stack = composableStack.get()
        val parentName = stack.lastOrNull()
        stack.add(traced.qualifiedName)

        // Skip framework composables for counting
        if (isFrameworkComposable(info)) return

        // Count total compositions for this key
        val totalCount = synchronized(compositionCountsLock) {
            val prev = compositionCounts.getOrPut(key) { 0 }
            val next = prev + 1
            compositionCounts[key] = next
            next
        }

        // If this is NOT the first composition (totalCount > 1), it's a recomposition
        if (totalCount > 1) {
            val recompCount = synchronized(recompositionCountsLock) {
                val prev = recompositionCounts.getOrPut(traced.qualifiedName) { 0 }
                val next = prev + 1
                recompositionCounts[traced.qualifiedName] = next
                next
            }

            // Build cause from pending state changes and dirty bits
            val stateCause = getPendingCause()
            val cause = if (stateCause != null) {
                stateCause.copy(isParameterDriven = dirty1 != 0)
            } else if (dirty1 != 0) {
                RecomposeCause(isParameterDriven = true)
            } else {
                null
            }

            // Also feed into the existing RecomposeTracker so tests work
            RecomposeTracker.recordCause(traced.qualifiedName, cause)

            // Record timestamped event with dirty bit info and parent causality
            val event = RecompositionEvent(
                timestampMs = currentTimeMillis(),
                dirty1 = dirty1,
                qualifiedName = traced.qualifiedName,
                parentName = parentName
            )
            synchronized(recompositionEventsLock) {
                recompositionEvents.getOrPut(traced.qualifiedName) { mutableListOf() }
                    .add(event)
            }

            if (isLoggingEnabled()) {
                val hasTags = synchronized(testTagToFunctionLock) {
                    testTagToFunction.values.any { it == traced.qualifiedName }
                }
                if (!hasTags) {
                    platformLog(TAG, "RECOMPOSE #$recompCount: ${traced.qualifiedName} (${traced.sourceLocation}) d1=$dirty1 parent=$parentName [no testTag — per-function aggregate]")
                }
            }
        }
    }

    override fun traceEventEnd() {
        val stack = composableStack.get()
        if (stack.isNotEmpty()) {
            stack.removeAt(stack.size - 1)
        }
    }

    // ── Parsing ──────────────────────────────────────────────────────

    /** Parse info string like "demo.app.ui.CounterValue (Counter.kt:29)". */
    internal fun parseInfo(key: Int, info: String): TracedComposable {
        val parenIdx = info.indexOf(" (")
        val qualifiedName = if (parenIdx > 0) info.substring(0, parenIdx) else info
        val sourceLocation = if (parenIdx > 0) info.substring(parenIdx + 2).trimEnd(')') else ""
        val simpleName = qualifiedName.substringAfterLast('.')

        val traced = TracedComposable(
            key = key,
            simpleName = simpleName,
            qualifiedName = qualifiedName,
            sourceLocation = sourceLocation,
            fullInfo = info
        )
        synchronized(simpleNameIndexLock) {
            simpleNameIndex.getOrPut(simpleName) { mutableListOf() }.add(traced)
        }
        return traced
    }

    /** Skip framework composables (androidx.*) for recomposition counting. */
    internal fun isFrameworkComposable(info: String): Boolean {
        return info.startsWith("androidx.") ||
               info.startsWith("kotlin.") ||
               info.startsWith("<get-") ||       // property accessors
               info.startsWith("remember(") || info == "remember" // remember calls (not user composables like rememberMyState)
    }

    // ── Name resolution (used by tag mapping) ────────────────────────

    /**
     * Given a Group name, determine if it represents a user composable and
     * return its resolved qualified name. Returns [fallback] if the name is
     * null, a framework composable, or otherwise not a user composable.
     */
    internal fun resolveUserComposable(groupName: String?, fallback: String?): String? {
        if (groupName == null) return fallback
        val isCandidate = !(groupName.startsWith("remember(") || groupName == "remember") &&
            frameworkPrefixes.none { groupName.startsWith(it) } &&
            groupName !in frameworkGroupNames &&
            groupName.isNotEmpty() && groupName.first().isUpperCase()
        if (!isCandidate) return fallback
        val resolved = resolveQualifiedName(groupName)
        return if (frameworkPrefixes.none { resolved.startsWith(it) }) resolved else fallback
    }

    /**
     * Resolve a simple composable name to its qualified name using the
     * traced composable info collected during traceEventStart calls.
     * Falls back to the simple name if no match is found.
     */
    internal fun resolveQualifiedName(simpleName: String): String {
        val matches = synchronized(simpleNameIndexLock) { simpleNameIndex[simpleName] }
        return matches?.firstOrNull()?.qualifiedName ?: simpleName
    }

    // ── Tag-to-function mapping (delegates to platform) ──────────────

    /**
     * Get the function name that a testTag maps to, or null if unknown.
     */
    fun getFunctionNameForTag(testTag: String): String? = synchronized(testTagToFunctionLock) {
        testTagToFunction[testTag]
    }

    /**
     * Walk the CompositionData Group trees to build a mapping from
     * Modifier.testTag values to the enclosing user composable function name.
     * Delegates to platform-specific implementation.
     */
    fun buildTagMapping(compositionData: Set<CompositionData>) {
        synchronized(lastSeenTagsLock) { lastSeenTags.clear() }
        platformBuildTagMapping(compositionData)
    }

    // ── Query APIs ───────────────────────────────────────────────────

    fun getRecompositionCount(name: String): Int {
        synchronized(recompositionCountsLock) {
            // Try exact match first (qualified name)
            recompositionCounts[name]?.let { return it }
            // Fallback: try as simple name (match by suffix)
            val matches = recompositionCounts.entries.filter {
                it.key.endsWith(".$name") || it.key == name
            }
            return when {
                matches.size == 1 -> matches.first().value
                matches.size > 1 -> {
                    if (isLoggingEnabled()) {
                        platformWarnLog(TAG, "Ambiguous composable name '$name' matches ${matches.size} entries; using first match")
                    }
                    matches.first().value
                }
                else -> 0
            }
        }
    }

    fun getAllRecompositionCounts(): Map<String, Int> {
        return synchronized(recompositionCountsLock) {
            recompositionCounts.toMap()
        }
    }

    fun getAllTracedComposables(): List<TracedComposable> {
        return synchronized(keyToInfoLock) {
            keyToInfo.values.toList()
        }
    }

    fun getRecompositionEvents(functionName: String): List<RecompositionEvent> {
        return synchronized(recompositionEventsLock) {
            recompositionEvents[functionName]?.toList() ?: emptyList()
        }
    }

    fun getPerTagRecompositionCount(tag: String): Int? = synchronized(perTagLock) {
        perTagRecompCounts[tag]
    }

    fun getPerTagRecompositionEvents(tag: String): List<RecompositionEvent> =
        synchronized(perTagLock) {
            perTagRecompEvents[tag]?.toList() ?: emptyList()
        }

    fun getParameterChanges(tag: String): List<List<ParameterChange>> =
        synchronized(tagParamLock) {
            tagParameterChanges[tag]?.toList() ?: emptyList()
        }

    /**
     * Returns true if the given function has multiple tags mapped to it,
     * indicating multiple instances of the same composable in the tree.
     */
    fun isMultiInstanceFunction(functionName: String): Boolean {
        synchronized(testTagToFunctionLock) {
            var count = 0
            for ((tag, value) in testTagToFunction) {
                if (value == functionName && tag in lastSeenTags) {
                    count++
                    if (count > 1) return true
                }
            }
            return false
        }
    }

    // ── Parameter diffing ────────────────────────────────────────────

    internal fun diffParams(old: List<ParamSnapshot>, new: List<ParamSnapshot>): List<ParameterChange> {
        val changes = mutableListOf<ParameterChange>()
        val oldByName = old.associateBy { it.name }
        val newByName = new.associateBy { it.name }

        for ((name, newSnap) in newByName) {
            val oldSnap = oldByName[name]
            if (oldSnap == null) {
                changes.add(ParameterChange(
                    parameterName = name ?: "<unnamed>",
                    oldValue = null,
                    newValue = newSnap.valueString,
                    changeType = ChangeType.ADDED
                ))
            } else if (oldSnap.valueHash != newSnap.valueHash) {
                val changeType = if (oldSnap.valueString == newSnap.valueString) {
                    ChangeType.REFERENCE_CHANGED
                } else {
                    ChangeType.VALUE_CHANGED
                }
                changes.add(ParameterChange(
                    parameterName = name ?: "<unnamed>",
                    oldValue = oldSnap.valueString,
                    newValue = newSnap.valueString,
                    changeType = changeType
                ))
            }
        }

        for ((name, oldSnap) in oldByName) {
            if (name !in newByName) {
                changes.add(ParameterChange(
                    parameterName = name ?: "<unnamed>",
                    oldValue = oldSnap.valueString,
                    newValue = null,
                    changeType = ChangeType.REMOVED
                ))
            }
        }

        return changes
    }

    // ── Reset ────────────────────────────────────────────────────────

    /**
     * Clears all tracked state — recomposition counts, composition history,
     * tag mappings, and parsed composable info.
     *
     * Use between independent tests to ensure full isolation.
     */
    fun reset() {
        resetCounts()
        synchronized(compositionCountsLock) { compositionCounts.clear() }
        synchronized(keyToInfoLock) { keyToInfo.clear() }
        synchronized(simpleNameIndexLock) { simpleNameIndex.clear() }
    }

    /**
     * Clears recomposition counts and tag mappings but preserves composition
     * history ([compositionCounts], [keyToInfo], [simpleNameIndex]).
     *
     * Use for mid-test resets where the same composition is still alive and
     * you want subsequent compositions of already-seen keys to still count
     * as recompositions rather than initial compositions.
     */
    fun resetCounts() {
        synchronized(recompositionCountsLock) { recompositionCounts.clear() }
        synchronized(recompositionEventsLock) { recompositionEvents.clear() }
        synchronized(testTagToFunctionLock) { testTagToFunction.clear() }
        synchronized(perTagLock) {
            perTagRecompCounts.clear()
            tagParamFingerprints.clear()
            perTagRecompEvents.clear()
        }
        synchronized(tagParamLock) {
            tagParamSnapshots.clear()
            tagParameterChanges.clear()
        }
        synchronized(lastSeenTagsLock) { lastSeenTags.clear() }
        synchronized(tagToIdentityLock) { tagToIdentity.clear() }
    }

    /**
     * Synchronously refresh the tag mapping from composition data.
     * Useful in tests where the asynchronous frame loop may not have
     * run yet when assertions are checked.
     */
    fun refreshTagMapping(compositionData: Set<CompositionData>) {
        buildTagMapping(compositionData)
    }

    /**
     * Returns a snapshot of the current composition data from the active activity.
     * Delegates to platform-specific runtime.
     */
    fun getCompositionSnapshots(): Set<CompositionData> =
        currentCompositionsSnapshot()
}
