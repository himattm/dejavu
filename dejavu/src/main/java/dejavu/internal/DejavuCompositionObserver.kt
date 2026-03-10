package dejavu.internal

import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.ObservableComposition
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implements the Compose [CompositionObserver] API to track per-scope state
 * dependencies and invalidation causality — information the existing
 * [DejavuTracer] and snapshot observer cannot provide.
 *
 * **What this adds over existing tracking:**
 * - The snapshot apply observer knows "a MutableState<Int> changed" globally.
 *   This observer knows "CounterValue reads MutableState@0x1a2b and was
 *   invalidated specifically by that object."
 * - Per-composable state dependency sets — which state objects each scope reads.
 *
 * Runs alongside [DejavuTracer] on the composition thread. Scope-to-name
 * correlation is done lazily in [onReadInScope]: by the time a state read
 * occurs in the composable body, [traceEventStart] has already pushed the
 * name onto `composableStack`. ([onScopeEnter] fires too early — during
 * `startRestartGroup`, before [traceEventStart].)
 *
 * Purely internal — used only to enrich error messages in
 * [dejavu.SemanticNodeInteractionsKt.failRecompositionsExpectation].
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
internal object DejavuCompositionObserver : CompositionObserver {

    @Volatile
    var isAvailable: Boolean = false

    // scope → qualified composable name
    private val scopeToComposable: MutableMap<RecomposeScope, String> =
        Collections.synchronizedMap(IdentityHashMap())

    // Scope from the most recent onScopeEnter, awaiting name from traceEventStart.
    // Thread-local because composition runs on a single thread.
    private val pendingScope = ThreadLocal<RecomposeScope?>()

    // scope → state objects read during last composition pass
    private val scopeReads: MutableMap<RecomposeScope, MutableSet<Any>> =
        Collections.synchronizedMap(IdentityHashMap())

    // qualifiedName → state objects this composable depends on (aggregated)
    private val composableDeps: MutableMap<String, MutableSet<Any>> =
        ConcurrentHashMap()

    // qualifiedName → invalidation records [(causeStateObject, timestampMs)]
    private val invalidations: MutableMap<String, MutableList<Invalidation>> =
        ConcurrentHashMap()

    internal data class Invalidation(
        val cause: Any?,
        val timestampMs: Long,
        val valueAtInvalidation: String?,
    )

    // identityHashCode → value string captured on first read (baseline before any invalidation)
    private val initialValues: ConcurrentHashMap<Int, String> = ConcurrentHashMap()

    // ── Callbacks ────────────────────────────────────────────────────

    override fun onBeginComposition(composition: ObservableComposition) {}
    override fun onEndComposition(composition: ObservableComposition) {}

    override fun onScopeEnter(scope: RecomposeScope) {
        // Name isn't available yet (startRestartGroup fires before traceEventStart).
        // Store as pending; bindPendingScope() will be called from traceEventStart.
        pendingScope.set(scope)
        scopeReads[scope]?.clear()
    }

    override fun onScopeExit(scope: RecomposeScope) {
        val name = scopeToComposable[scope] ?: return
        val reads = scopeReads[scope] ?: return
        if (reads.isNotEmpty()) {
            composableDeps.getOrPut(name) { ConcurrentHashMap.newKeySet() }.addAll(reads)
        }
    }

    override fun onReadInScope(scope: RecomposeScope, state: Any) {
        scopeReads.getOrPut(scope) {
            Collections.newSetFromMap(IdentityHashMap())
        }.add(state)
        // Capture baseline value on first read so we can show the full progression
        val id = System.identityHashCode(state)
        if (!initialValues.containsKey(id)) {
            describeStateValue(state)?.let { initialValues[id] = it }
        }
    }

    /**
     * Called from [DejavuTracer.traceEventStart] immediately after pushing the
     * composable name onto the stack. Binds the pending scope (from the most
     * recent [onScopeEnter]) to the given name.
     */
    fun bindPendingScope(qualifiedName: String) {
        val scope = pendingScope.get() ?: return
        pendingScope.set(null)
        scopeToComposable[scope] = qualifiedName
    }

    override fun onScopeInvalidated(scope: RecomposeScope, cause: Any?) {
        val name = scopeToComposable[scope] ?: return
        val value = cause?.let { describeStateValue(it) }
        invalidations.getOrPut(name) { CopyOnWriteArrayList() }
            .add(Invalidation(cause, System.currentTimeMillis(), value))
    }

    override fun onScopeDisposed(scope: RecomposeScope) {
        scopeToComposable.remove(scope)
        scopeReads.remove(scope)
    }

    // ── Query (for error messages) ──────────────────────────────────

    /**
     * Formats a human-readable description of the state that directly
     * caused invalidation for this composable. Returns null if no
     * invalidation was recorded.
     */
    fun describeInvalidationCauses(qualifiedName: String): String? {
        val records = invalidations[qualifiedName] ?: return null
        if (records.isEmpty()) return null

        // Group by state object identity so repeated invalidations from the same
        // state show as one line with a value progression.
        data class GroupKey(val identityHash: Int, val isNull: Boolean)

        val grouped = records.groupBy {
            if (it.cause != null) GroupKey(System.identityHashCode(it.cause), false)
            else GroupKey(0, true)
        }

        return buildString {
            appendLine("  Invalidation causes:")
            for ((key, group) in grouped) {
                if (key.isNull) {
                    appendLine("    (forced/unknown cause) × ${group.size}")
                    continue
                }
                val cause = group.first().cause!!
                val typeName = friendlyStateName(cause)
                val times = group.size
                val timesStr = if (times == 1) "1 time" else "$times times"

                // Build value progression: initial → v1 → v2 → v3
                val values = mutableListOf<String>()
                val initial = initialValues[key.identityHash]
                if (initial != null) values.add(initial)
                for (inv in group) {
                    val v = inv.valueAtInvalidation ?: continue
                    // Skip duplicates from the previous entry in the progression
                    if (values.isEmpty() || values.last() != v) values.add(v)
                }

                append("    $typeName changed $timesStr")
                if (values.size >= 2) {
                    append(": ${values.joinToString(" → ")}")
                } else if (values.size == 1) {
                    append(" (current value: ${values.first()})")
                }
                appendLine()
            }
        }.trimEnd()
    }

    /**
     * Formats the set of state objects this composable reads.
     * Returns null if no reads were recorded.
     */
    fun describeStateDependencies(qualifiedName: String): String? {
        val deps = composableDeps[qualifiedName] ?: return null
        if (deps.isEmpty()) return null

        val descriptions = deps.map { friendlyStateName(it) }.sorted()
        return "  State reads: ${descriptions.joinToString(", ")}"
    }

    fun reset() {
        // Keep scopeToComposable — it's structural (scope → name) and must persist
        // so that onScopeInvalidated (which fires before recomposition) can resolve
        // scope names. Cleared only in fullReset() when the observer is disposed.
        scopeReads.clear()
        composableDeps.clear()
        invalidations.clear()
        initialValues.clear()
    }

    /** Full cleanup when the observer is being disposed. */
    fun fullReset() {
        scopeToComposable.clear()
        pendingScope.remove()
        reset()
    }

    // ── Internals ───────────────────────────────────────────────────

    /**
     * Returns a human-friendly name like `MutableState<Int>` instead of
     * `SnapshotMutableStateImpl`.
     */
    private fun friendlyStateName(state: Any): String {
        val valueType = try {
            when (state) {
                is MutableState<*> -> state.value?.let { it::class.simpleName }
                else -> {
                    val getter = state.javaClass.methods.find {
                        it.name == "getValue" && it.parameterCount == 0
                    }
                    getter?.invoke(state)?.let { it::class.simpleName }
                }
            }
        } catch (_: Throwable) { null }

        val rawName = state::class.simpleName ?: "State"
        return when {
            rawName.contains("Derived") -> "DerivedState<${valueType ?: "?"}>"
            state is MutableState<*> -> "MutableState<${valueType ?: "?"}>"
            rawName.contains("Mutable") || rawName.contains("Snapshot") ->
                "MutableState<${valueType ?: "?"}>"
            else -> if (valueType != null) "$rawName<$valueType>" else rawName
        }
    }

    private fun describeStateValue(state: Any): String? {
        return try {
            when (state) {
                is MutableState<*> -> state.value?.let { v ->
                    val s = v.toString()
                    if (s.length > 60) s.take(57) + "..." else s
                }
                else -> {
                    val getter = state.javaClass.methods.find {
                        it.name == "getValue" && it.parameterCount == 0
                    }
                    getter?.invoke(state)?.let { v ->
                        val s = v.toString()
                        if (s.length > 60) s.take(57) + "..." else s
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }
}
