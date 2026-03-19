package dejavu.internal

import android.util.Log
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import kotlinx.atomicfu.locks.synchronized

/**
 * Android-specific tag-mapping implementation that walks the Compose Group tree
 * to map `Modifier.testTag` values to enclosing user composable function names.
 *
 * This object accesses [DejavuTracer]'s internal state maps to read/write
 * tag and recomposition data.
 */
internal object TagMapping {
    private const val TAG = "Dejavu"

    /**
     * Walk the CompositionData Group trees to build a mapping from
     * Modifier.testTag values to the enclosing user composable function name.
     *
     * SubcomposeLayout (used by LazyColumn, BoxWithConstraints, etc.) creates
     * separate sub-compositions. A testTag modifier may appear in the
     * sub-composition's Group tree while the enclosing user composable lives in
     * the parent composition's tree. To handle this, we run two passes:
     *   1. Normal walk -- maps tags that have a user composable ancestor.
     *   2. Fallback pass -- for any tags found without a user composable ancestor
     *      (sub-composition boundary), search ALL Group trees for a user
     *      composable whose descendant subtree contains the unmapped tag.
     */
    @OptIn(UiToolingDataApi::class)
    fun buildTagMapping(compositionData: Set<CompositionData>) {
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
        synchronized(DejavuTracer.testTagToFunctionLock) {
            unmappedTags.removeAll(DejavuTracer.testTagToFunction.keys)
        }

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
        val currentUserComposable = DejavuTracer.resolveUserComposable(group.name, enclosingUserComposable)
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
                                    synchronized(DejavuTracer.testTagToFunctionLock) {
                                        DejavuTracer.testTagToFunction.putIfAbsent(tag, currentUserComposable)
                                    }
                                    synchronized(DejavuTracer.lastSeenTagsLock) {
                                        DejavuTracer.lastSeenTags.add(tag)
                                    }
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
                                                        synchronized(DejavuTracer.testTagToFunctionLock) {
                                                            DejavuTracer.testTagToFunction.putIfAbsent(value, currentUserComposable)
                                                        }
                                                        synchronized(DejavuTracer.lastSeenTagsLock) {
                                                            DejavuTracer.lastSeenTags.add(value)
                                                        }
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
                                        synchronized(DejavuTracer.testTagToFunctionLock) {
                                            DejavuTracer.testTagToFunction.putIfAbsent(tagValue, currentUserComposable)
                                        }
                                        synchronized(DejavuTracer.lastSeenTagsLock) {
                                            DejavuTracer.lastSeenTags.add(tagValue)
                                        }
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
                                    synchronized(DejavuTracer.testTagToFunctionLock) {
                                        DejavuTracer.testTagToFunction.putIfAbsent(tagValue, currentUserComposable)
                                    }
                                    synchronized(DejavuTracer.lastSeenTagsLock) {
                                        DejavuTracer.lastSeenTags.add(tagValue)
                                    }
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
                if (isLoggingEnabled() && !foundTag) {
                    val cn = mi.modifier.javaClass.name
                    if (cn.contains("TestTag", ignoreCase = true) || cn.contains("Semantics", ignoreCase = true)) {
                        Log.d(TAG, "Unresolved testTag on $currentUserComposable (modifier=${cn.substringAfterLast('.')})")
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

        val currentUserComposable = DejavuTracer.resolveUserComposable(group.name, enclosingUserComposable)
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
                                    synchronized(DejavuTracer.testTagToFunctionLock) {
                                        DejavuTracer.testTagToFunction.putIfAbsent(tag, currentUserComposable)
                                    }
                                    synchronized(DejavuTracer.lastSeenTagsLock) {
                                        DejavuTracer.lastSeenTags.add(tag)
                                    }
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

    @OptIn(UiToolingDataApi::class)
    private fun detectTagRecomposition(tag: String, userGroup: Group?, parentName: String? = null) {
        if (userGroup == null) return

        // Store Group.identity (Anchor) for future per-instance tracking
        val identity = userGroup.identity
        if (identity != null) {
            synchronized(DejavuTracer.tagToIdentityLock) {
                DejavuTracer.tagToIdentity[tag] = identity
            }
        }

        // Capture current parameter snapshot for change tracking
        val currentSnapshot = captureParamSnapshot(userGroup)

        val fingerprint = computeParamFingerprint(userGroup)
        val prev = synchronized(DejavuTracer.perTagLock) {
            DejavuTracer.tagParamFingerprints.put(tag, fingerprint)
        }

        // Mark this tag as having a reliable fingerprint comparison baseline,
        // but only during frame-loop passes.
        if (prev != null && DejavuTracer.isFrameLoopPass) {
            synchronized(DejavuTracer.tagsWithFingerprintLock) {
                DejavuTracer.tagsWithFingerprint.add(tag)
            }
        }

        // Store/update the snapshot
        val previousSnapshot = synchronized(DejavuTracer.tagParamLock) {
            DejavuTracer.tagParamSnapshots.put(tag, currentSnapshot)
        }

        if (prev != null && prev != fingerprint) {
            val count = synchronized(DejavuTracer.perTagLock) {
                val prevCount = DejavuTracer.perTagRecompCounts.getOrPut(tag) { 0 }
                val next = prevCount + 1
                DejavuTracer.perTagRecompCounts[tag] = next
                next
            }
            val functionName = synchronized(DejavuTracer.testTagToFunctionLock) {
                DejavuTracer.testTagToFunction[tag]
            }

            if (isLoggingEnabled() && functionName != null) {
                val sourceLocation = synchronized(DejavuTracer.simpleNameIndexLock) {
                    DejavuTracer.simpleNameIndex[functionName.substringAfterLast('.')]
                }?.firstOrNull()?.sourceLocation ?: ""
                val locationSuffix = if (sourceLocation.isNotEmpty()) " ($sourceLocation)" else ""
                val parentSuffix = if (parentName != null) ", parent=${parentName}" else ""
                platformLog(TAG, "Recomposition #$count: [$tag] $functionName$locationSuffix$parentSuffix")
            }

            // Diff parameter snapshots to determine what changed
            if (previousSnapshot != null) {
                val paramChanges = DejavuTracer.diffParams(previousSnapshot, currentSnapshot)
                if (paramChanges.isNotEmpty()) {
                    synchronized(DejavuTracer.tagParamLock) {
                        DejavuTracer.tagParameterChanges.getOrPut(tag) { mutableListOf() }
                            .add(paramChanges)
                    }
                }
            }

            // Approximate dirty bits from function-level events
            val approximateDirty1 = functionName?.let {
                synchronized(DejavuTracer.recompositionEventsLock) {
                    DejavuTracer.recompositionEvents[it]?.lastOrNull()?.dirty1
                }
            } ?: 0

            val event = DejavuTracer.RecompositionEvent(
                timestampMs = currentTimeMillis(),
                dirty1 = approximateDirty1,
                qualifiedName = functionName ?: tag,
                parentName = parentName
            )
            synchronized(DejavuTracer.perTagLock) {
                DejavuTracer.perTagRecompEvents.getOrPut(tag) { mutableListOf() }
                    .add(event)
            }
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

    @OptIn(UiToolingDataApi::class)
    private fun computeParamFingerprint(group: Group): Int {
        var hash = 17
        for (p in group.parameters) {
            hash = hash * 31 + (p.name?.hashCode() ?: 0)
            hash = hash * 31 + (p.value?.hashCode() ?: 0)
        }
        // Also include data to catch state-driven changes
        for (item in group.data) {
            if (item != null && item.javaClass.name.startsWith("androidx.compose.runtime.")) continue
            hash = hash * 31 + (item?.hashCode() ?: 0)
        }
        return hash
    }
}
