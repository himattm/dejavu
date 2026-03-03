package dejavu.internal

import android.util.Log
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@OptIn(InternalComposeTracingApi::class)
internal object DejavuTracer : CompositionTracer {
    private const val TAG = "DejavuTracer"

    @Volatile
    var enabled = false

    // Count traceEventStart calls per composable key (int)
    // First call is initial composition, subsequent are recompositions
    private val compositionCounts = ConcurrentHashMap<Int, AtomicInteger>()

    // Map from key (int) to parsed info: functionName, fullInfo, sourceLocation
    private val keyToInfo = ConcurrentHashMap<Int, TracedComposable>()

    // Reverse index: simple name -> traced composables for O(1) qualified-name lookup
    private val simpleNameIndex = ConcurrentHashMap<String, MutableList<TracedComposable>>()

    // Map from qualified function name to recomposition count (excludes initial composition)
    // This is the primary query API; query methods support simple name fallback
    private val recompositionCounts = ConcurrentHashMap<String, AtomicInteger>()

    // Timestamped recomposition events per qualified function name
    private val recompositionEvents = ConcurrentHashMap<String, MutableList<RecompositionEvent>>()

    // Per-tag recomposition counting via Group tree parameter change detection
    private val perTagRecompCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val tagParamFingerprints = ConcurrentHashMap<String, Int>()
    private val perTagRecompEvents = ConcurrentHashMap<String, MutableList<RecompositionEvent>>()

    // Parameter snapshots for change tracking (replaces raw fingerprint comparison)
    private val tagParamSnapshots = ConcurrentHashMap<String, List<ParamSnapshot>>()
    private val tagParameterChanges = ConcurrentHashMap<String, MutableList<List<ParameterChange>>>()

    // Tags seen during the most recent buildTagMapping pass; used to detect stale entries
    private val lastSeenTags: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // Stable per-instance identifier (Group.identity / Anchor) for each tag
    private val tagToIdentity = ConcurrentHashMap<String, Any>()

    // Track parent-child causality: stack of qualified names for the current composition
    // minSdk 21 but API 26+ expected at test runtime
    private val composableStack = object : ThreadLocal<ArrayDeque<String>>() {
        override fun initialValue(): ArrayDeque<String> = ArrayDeque()
    }

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

    override fun isTraceInProgress(): Boolean = enabled

    override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
        // Parse and cache info
        val traced = keyToInfo.getOrPut(key) { parseInfo(key, info) }

        // Push onto the composable stack BEFORE the framework check so that
        // traceEventEnd (which fires for every composable) can always pop.
        val stack = composableStack.get()!!
        val parentName = stack.lastOrNull()
        stack.add(traced.qualifiedName)

        // Skip framework composables for counting
        if (isFrameworkComposable(info)) return

        // Count total compositions for this key
        val totalCount = compositionCounts.getOrPut(key) { AtomicInteger(0) }.incrementAndGet()

        // If this is NOT the first composition (totalCount > 1), it's a recomposition
        if (totalCount > 1) {
            val recompCount = recompositionCounts.getOrPut(traced.qualifiedName) { AtomicInteger(0) }.incrementAndGet()

            // Build cause from pending state changes and dirty bits
            val stateCause = Runtime.getPendingCause()
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
            val events = recompositionEvents.getOrPut(traced.qualifiedName) {
                java.util.concurrent.CopyOnWriteArrayList()
            }
            events.add(RecompositionEvent(
                timestampMs = System.currentTimeMillis(),
                dirty1 = dirty1,
                qualifiedName = traced.qualifiedName,
                parentName = parentName
            ))

            if (Runtime.isLoggingEnabled) {
                val hasTags = testTagToFunction.values.any { it == traced.qualifiedName }
                if (!hasTags) {
                    Log.d(TAG, "RECOMPOSE #$recompCount: ${traced.qualifiedName} (${traced.sourceLocation}) d1=$dirty1 parent=$parentName [no testTag — per-function aggregate]")
                }
            }
        }
    }

    override fun traceEventEnd() {
        val stack = composableStack.get() ?: return
        if (stack.isNotEmpty()) {
            stack.removeAt(stack.size - 1)
        }
    }

    // Parse info string like "demo.app.ui.CounterValue (Counter.kt:29)"
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
        simpleNameIndex.getOrPut(simpleName) { mutableListOf() }.add(traced)
        return traced
    }

    // Skip framework composables (androidx.*) for recomposition counting
    internal fun isFrameworkComposable(info: String): Boolean {
        return info.startsWith("androidx.") ||
               info.startsWith("kotlin.") ||
               info.startsWith("<get-") ||       // property accessors
               info.startsWith("remember(") || info == "remember" // remember calls (not user composables like rememberMyState)
    }

    // ── Tag-to-function mapping ──────────────────────────────────────

    // Cache of testTag -> composable function name
    private val testTagToFunction = ConcurrentHashMap<String, String>()

    // Framework packages/files to skip when walking the Group tree
    private val frameworkPrefixes = setOf("androidx.", "kotlin.", "android.")

    // Compose Material/Foundation composable names that appear as Group names in the composition tree.
    // User composables with these names should use their full qualified name for disambiguation.
    private val frameworkGroupNames = setOf(
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

    /**
     * Walk the CompositionData Group trees to build a mapping from
     * Modifier.testTag values to the enclosing user composable function name.
     *
     * SubcomposeLayout (used by LazyColumn, BoxWithConstraints, etc.) creates
     * separate sub-compositions. A testTag modifier may appear in the
     * sub-composition's Group tree while the enclosing user composable lives in
     * the parent composition's tree. To handle this, we run two passes:
     *   1. Normal walk — maps tags that have a user composable ancestor.
     *   2. Fallback pass — for any tags found without a user composable ancestor
     *      (sub-composition boundary), search ALL Group trees for a user
     *      composable whose descendant subtree contains the unmapped tag.
     */
    @OptIn(UiToolingDataApi::class)
    fun buildTagMapping(compositionData: Set<CompositionData>) {
        lastSeenTags.clear()
        val unmappedTags = mutableSetOf<String>()
        val trees = mutableListOf<Group>()

        // First pass: standard walk, cache trees for potential second pass
        compositionData.forEach { data ->
            try {
                val root: Group = data.asTree()
                trees.add(root)
                walkForTagMapping(root, null, unmappedTags, null, enclosingUserGroup = null, parentUserComposable = null)
            } catch (e: NoClassDefFoundError) {
                Log.e(TAG, "Missing: androidx.compose.ui:ui-tooling-data. " +
                    "Ensure the dejavu dependency is included correctly in your build.")
            } catch (_: Throwable) {
                // Gracefully handle unexpected tree structures
            }
        }

        // Remove tags that were successfully mapped in the first pass
        unmappedTags.removeAll(testTagToFunction.keys)

        // Second pass: for unmapped tags, search cached trees with user composable context
        if (unmappedTags.isNotEmpty()) {
            for (root in trees) {
                if (unmappedTags.isEmpty()) break
                try {
                    resolveUnmappedTags(root, null, unmappedTags, null, enclosingUserGroup = null, parentUserComposable = null)
                } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Given a Group name, determine if it represents a user composable and
     * return its resolved qualified name. Returns [fallback] if the name is
     * null, a framework composable, or otherwise not a user composable.
     */
    private fun resolveUserComposable(groupName: String?, fallback: String?): String? {
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
    private fun resolveQualifiedName(simpleName: String): String {
        val matches = simpleNameIndex[simpleName] ?: return simpleName
        return matches.firstOrNull()?.qualifiedName ?: simpleName
    }

    @OptIn(UiToolingDataApi::class)
    private fun extractIntKey(groupKey: Any?): Int? = when (groupKey) {
        is Int -> groupKey
        else -> null
    }

    @OptIn(UiToolingDataApi::class)
    private fun walkForTagMapping(
        group: Group,
        enclosingUserComposable: String?,
        unmappedTags: MutableSet<String>,
        enclosingUserComposableKey: Int?,
        enclosingUserGroup: Group? = null,
        parentUserComposable: String? = null
    ) {
        val currentUserComposable = resolveUserComposable(group.name, enclosingUserComposable)
        val bodyKey = extractIntKey(group.key)

        // Track the Group node for the current user composable (for parameter fingerprinting)
        val currentUserGroup = if (currentUserComposable != enclosingUserComposable && currentUserComposable != null) {
            group
        } else {
            enclosingUserGroup
        }

        // When the user composable changes, the old enclosing becomes the parent
        val currentParentUserComposable = if (currentUserComposable != enclosingUserComposable && currentUserComposable != null) {
            enclosingUserComposable
        } else {
            parentUserComposable
        }

        val currentKey = if (currentUserComposable != enclosingUserComposable && currentUserComposable != null) {
            bodyKey ?: enclosingUserComposableKey
        } else {
            enclosingUserComposableKey
        }

        // Check modifiers for testTag
        if (currentUserComposable != null) {
            for (mi in group.modifierInfo) {
                var foundTag = false

                // Approach 0: InspectableValue API (Compose 1.3+ ModifierNodeElement)
                try {
                    val modifier = mi.modifier
                    if (modifier is InspectableValue) {
                        if (modifier.nameFallback == "testTag") {
                            for (element in modifier.inspectableElements) {
                                if (element.name == "tag" && element.value is String) {
                                    val tag = element.value as String
                                    testTagToFunction.putIfAbsent(tag, currentUserComposable)
                                    lastSeenTags.add(tag)
                                    detectTagRecomposition(tag, currentUserGroup, currentParentUserComposable)
                                    foundTag = true
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {}

                if (!foundTag) {
                    try {
                        // Approach 1: Try SemanticsModifier to read TestTag
                        val modifier = mi.modifier
                        val semModClass = modifier.javaClass.interfaces.find {
                            it.simpleName == "SemanticsModifier"
                        }
                        if (semModClass != null) {
                            // Try to get semanticsConfiguration
                            val configMethod = modifier.javaClass.methods.find {
                                it.name == "getSemanticsConfiguration" && it.parameterCount == 0
                            }
                            if (configMethod != null) {
                                val config = configMethod.invoke(modifier)
                                if (config != null) {
                                    // Try to iterate the config for TestTag
                                    val iteratorMethod = config.javaClass.methods.find {
                                        it.name == "iterator" && it.parameterCount == 0
                                    }
                                    if (iteratorMethod != null) {
                                        val iter = iteratorMethod.invoke(config) as? Iterator<*>
                                        iter?.let {
                                            while (it.hasNext()) {
                                                val entry = it.next() ?: continue
                                                val keyField = entry.javaClass.methods.find { m -> m.name == "getKey" }
                                                val valueField = entry.javaClass.methods.find { m -> m.name == "getValue" }
                                                if (keyField != null && valueField != null) {
                                                    val key = keyField.invoke(entry)
                                                    val value = valueField.invoke(entry)
                                                    if (key != null && key.toString().contains("TestTag") && value is String) {
                                                        testTagToFunction.putIfAbsent(value, currentUserComposable)
                                                        lastSeenTags.add(value)
                                                        detectTagRecomposition(value, currentUserGroup, currentParentUserComposable)
                                                        foundTag = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        // Swallow reflection errors
                    }
                }

                if (!foundTag) {
                    try {
                        // Approach 2: Check modifier class name for TestTag patterns
                        val modClassName = mi.modifier.javaClass.simpleName
                        if (modClassName.contains("TestTag") || modClassName.contains("testTag")) {
                            // Try to extract tag value via reflection
                            val fields = mi.modifier.javaClass.declaredFields
                            for (field in fields) {
                                if (field.name == "tag" || field.name == "value" || field.name == "testTag") {
                                    field.isAccessible = true
                                    val tagValue = field.get(mi.modifier) as? String
                                    if (tagValue != null) {
                                        testTagToFunction.putIfAbsent(tagValue, currentUserComposable)
                                        lastSeenTags.add(tagValue)
                                        detectTagRecomposition(tagValue, currentUserGroup, currentParentUserComposable)
                                        foundTag = true
                                    }
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        // Swallow reflection errors
                    }
                }

                if (!foundTag) {
                    try {
                        // Approach 3: toString() fallback
                        val modStr = mi.modifier.toString()
                        if (modStr.contains("TestTag")) {
                            val tagField = mi.modifier.javaClass.declaredFields.find {
                                it.name == "tag" || it.name == "value"
                            }
                            if (tagField != null) {
                                tagField.isAccessible = true
                                val tagValue = tagField.get(mi.modifier) as? String
                                if (tagValue != null) {
                                    testTagToFunction.putIfAbsent(tagValue, currentUserComposable)
                                    lastSeenTags.add(tagValue)
                                    detectTagRecomposition(tagValue, currentUserGroup, currentParentUserComposable)
                                    foundTag = true
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        // Swallow
                    }
                }

                // Diagnostic logging for failed tag resolution
                if (Runtime.isLoggingEnabled && !foundTag) {
                    val cn = mi.modifier.javaClass.name
                    if (cn.contains("TestTag", ignoreCase = true) || cn.contains("Semantics", ignoreCase = true)) {
                        Log.w(TAG, "Modifier looks like testTag but extraction failed: $cn")
                    }
                }
            }
        } else {
            // No user composable ancestor — we are likely inside a sub-composition.
            // Extract any tag values and record them as unmapped so the second pass
            // can try to resolve them from a parent CompositionData tree.
            for (mi in group.modifierInfo) {
                try {
                    val modifier = mi.modifier
                    if (modifier is InspectableValue && modifier.nameFallback == "testTag") {
                        for (element in modifier.inspectableElements) {
                            if (element.name == "tag" && element.value is String) {
                                unmappedTags.add(element.value as String)
                            }
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        // Recurse into children
        for (child in group.children) {
            walkForTagMapping(child, currentUserComposable, unmappedTags, currentKey, enclosingUserGroup = currentUserGroup, parentUserComposable = currentParentUserComposable)
        }
    }

    /**
     * Second-pass resolution for tags found in sub-compositions without a user
     * composable ancestor. Walks the Group tree in a single O(N) pass,
     * checking each node's modifiers inline instead of re-walking the subtree
     * from every user composable (which was O(N^2)).
     */
    @OptIn(UiToolingDataApi::class)
    private fun resolveUnmappedTags(
        group: Group,
        enclosingUserComposable: String?,
        unmappedTags: MutableSet<String>,
        enclosingUserComposableKey: Int?,
        enclosingUserGroup: Group? = null,
        parentUserComposable: String? = null
    ) {
        if (unmappedTags.isEmpty()) return

        val currentUserComposable = resolveUserComposable(group.name, enclosingUserComposable)
        val bodyKey = extractIntKey(group.key)

        // Track the Group node for the current user composable (for parameter fingerprinting)
        val currentUserGroup = if (currentUserComposable != enclosingUserComposable && currentUserComposable != null) {
            group
        } else {
            enclosingUserGroup
        }

        // When the user composable changes, the old enclosing becomes the parent
        val currentParentUserComposable = if (currentUserComposable != enclosingUserComposable && currentUserComposable != null) {
            enclosingUserComposable
        } else {
            parentUserComposable
        }

        val currentKey = if (currentUserComposable != enclosingUserComposable && currentUserComposable != null) {
            bodyKey ?: enclosingUserComposableKey
        } else {
            enclosingUserComposableKey
        }

        // Check modifiers on THIS group for unmapped tags
        if (currentUserComposable != null) {
            for (mi in group.modifierInfo) {
                try {
                    val modifier = mi.modifier
                    if (modifier is InspectableValue && modifier.nameFallback == "testTag") {
                        for (element in modifier.inspectableElements) {
                            if (element.name == "tag" && element.value is String) {
                                val tag = element.value as String
                                if (tag in unmappedTags) {
                                    testTagToFunction.putIfAbsent(tag, currentUserComposable)
                                    lastSeenTags.add(tag)
                                    detectTagRecomposition(tag, currentUserGroup, currentParentUserComposable)
                                    unmappedTags.remove(tag)
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        // Recurse into children
        for (child in group.children) {
            resolveUnmappedTags(child, currentUserComposable, unmappedTags, currentKey, enclosingUserGroup = currentUserGroup, parentUserComposable = currentParentUserComposable)
        }
    }

    /**
     * Get the function name that a testTag maps to, or null if unknown.
     */
    fun getFunctionNameForTag(testTag: String): String? = testTagToFunction[testTag]

    // ── Query APIs ───────────────────────────────────────────────────

    fun getRecompositionCount(name: String): Int {
        // Try exact match first (qualified name)
        recompositionCounts[name]?.get()?.let { return it }
        // Fallback: try as simple name (match by suffix)
        val matches = recompositionCounts.entries.filter {
            it.key.endsWith(".$name") || it.key == name
        }
        return when {
            matches.size == 1 -> matches.first().value.get()
            matches.size > 1 -> {
                if (Runtime.isLoggingEnabled) {
                    Log.w(TAG, "Ambiguous composable name '$name' matches ${matches.size} entries; using first match")
                }
                matches.first().value.get()
            }
            else -> 0
        }
    }

    fun getAllRecompositionCounts(): Map<String, Int> {
        return recompositionCounts.mapValues { it.value.get() }
    }

    fun getAllTracedComposables(): List<TracedComposable> {
        return keyToInfo.values.toList()
    }

    fun getRecompositionEvents(functionName: String): List<RecompositionEvent> {
        return recompositionEvents[functionName]?.toList() ?: emptyList()
    }

    fun getPerTagRecompositionCount(tag: String): Int? =
        perTagRecompCounts[tag]?.get()

    fun getPerTagRecompositionEvents(tag: String): List<RecompositionEvent> =
        perTagRecompEvents[tag]?.toList() ?: emptyList()

    fun getParameterChanges(tag: String): List<List<ParameterChange>> =
        tagParameterChanges[tag]?.toList() ?: emptyList()

    /**
     * Returns true if the given function has multiple tags mapped to it,
     * indicating multiple instances of the same composable in the tree.
     */
    fun isMultiInstanceFunction(functionName: String): Boolean {
        var count = 0
        for ((tag, value) in testTagToFunction) {
            if (value == functionName && tag in lastSeenTags) {
                count++
                if (count > 1) return true
            }
        }
        return false
    }

    @OptIn(UiToolingDataApi::class)
    private fun detectTagRecomposition(tag: String, userGroup: Group?, parentName: String? = null) {
        if (userGroup == null) return

        // Feature 1d: Store Group.identity (Anchor) for future per-instance tracking
        val identity = userGroup.identity
        if (identity != null) {
            tagToIdentity[tag] = identity
        }

        // Capture current parameter snapshot for change tracking
        val currentSnapshot = captureParamSnapshot(userGroup)

        val fingerprint = computeParamFingerprint(userGroup)
        val prev = tagParamFingerprints.put(tag, fingerprint)

        // Store/update the snapshot
        val previousSnapshot = tagParamSnapshots.put(tag, currentSnapshot)

        if (prev != null && prev != fingerprint) {
            val count = perTagRecompCounts.getOrPut(tag) { AtomicInteger(0) }.incrementAndGet()
            val functionName = testTagToFunction[tag]

            if (Runtime.isLoggingEnabled && functionName != null) {
                val sourceLocation = simpleNameIndex[functionName.substringAfterLast('.')]
                    ?.firstOrNull()?.sourceLocation ?: ""
                val locationSuffix = if (sourceLocation.isNotEmpty()) " ($sourceLocation)" else ""
                Log.d(TAG, "RECOMPOSE-TAG #$count: $tag → $functionName$locationSuffix parent=$parentName")
            }

            // Diff parameter snapshots to determine what changed
            if (previousSnapshot != null) {
                val paramChanges = diffParams(previousSnapshot, currentSnapshot)
                if (paramChanges.isNotEmpty()) {
                    tagParameterChanges.getOrPut(tag) {
                        java.util.concurrent.CopyOnWriteArrayList()
                    }.add(paramChanges)
                }
            }

            // Feature 1c: Approximate dirty bits from function-level events
            val approximateDirty1 = functionName?.let {
                recompositionEvents[it]?.lastOrNull()?.dirty1
            } ?: 0

            val events = perTagRecompEvents.getOrPut(tag) {
                java.util.concurrent.CopyOnWriteArrayList()
            }
            events.add(RecompositionEvent(
                timestampMs = System.currentTimeMillis(),
                dirty1 = approximateDirty1,
                qualifiedName = functionName ?: tag,
                parentName = parentName
            ))
        }
    }

    @OptIn(UiToolingDataApi::class)
    private fun captureParamSnapshot(group: Group): List<ParamSnapshot> {
        return group.parameters.map { p ->
            val valueStr = try {
                p.value?.toString()?.take(80)
            } catch (_: Throwable) {
                null
            }
            ParamSnapshot(
                name = p.name,
                valueHash = p.value?.hashCode() ?: 0,
                valueString = valueStr
            )
        }
    }

    private fun diffParams(old: List<ParamSnapshot>, new: List<ParamSnapshot>): List<ParameterChange> {
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

    @OptIn(UiToolingDataApi::class)
    private fun computeParamFingerprint(group: Group): Int {
        var hash = 17
        for (p in group.parameters) {
            hash = hash * 31 + (p.name?.hashCode() ?: 0)
            hash = hash * 31 + (p.value?.hashCode() ?: 0)
        }
        // Also include data to catch state-driven changes
        for (item in group.data) {
            hash = hash * 31 + (item?.hashCode() ?: 0)
        }
        return hash
    }

    fun reset() {
        // Only clear the recomposition counts (query API).
        // Keep compositionCounts and keyToInfo so that already-composed keys
        // are still recognized as "seen" — the next traceEventStart for them
        // will correctly count as a recomposition rather than initial composition.
        recompositionCounts.clear()
        recompositionEvents.clear()
        testTagToFunction.clear()
        perTagRecompCounts.clear()
        tagParamFingerprints.clear()
        perTagRecompEvents.clear()
        tagParamSnapshots.clear()
        tagParameterChanges.clear()
        lastSeenTags.clear()
        tagToIdentity.clear()
        // Note: simpleNameIndex is NOT cleared here because keyToInfo is kept.
        // simpleNameIndex is derived from keyToInfo entries (populated in parseInfo),
        // and since keyToInfo is preserved across reset so that already-composed keys
        // are still recognized, the reverse index must stay in sync.
    }

    /**
     * Synchronously refresh the tag mapping from composition data.
     * Useful in tests where the asynchronous frame loop may not have
     * run yet when assertions are checked.
     */
    @OptIn(UiToolingDataApi::class)
    fun refreshTagMapping(compositionData: Set<CompositionData>) {
        buildTagMapping(compositionData)
    }

    /**
     * Returns a snapshot of the current composition data from the active activity.
     * Delegates to [Runtime.currentCompositionsSnapshot].
     */
    fun getCompositionSnapshots(): Set<CompositionData> =
        Runtime.currentCompositionsSnapshot()
}
