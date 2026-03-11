package dejavu.internal

/**
 * Reflection bridge for [DejavuCompositionObserver] and [CompositionRegistrar].
 *
 * These classes implement Compose runtime observer interfaces that were added
 * in Compose 1.7+. To maintain compilation against older Compose BOMs, they
 * live in a separate source set (`src/observer/`) that is only compiled when
 * the observer API is on the classpath. This bridge uses reflection so that
 * code in the main source set can call observer methods without a compile-time
 * dependency on the observer classes.
 */
internal object ObserverCompat {

    private val observerInstance: Any? by lazy {
        try {
            Class.forName("dejavu.internal.DejavuCompositionObserver")
                .getDeclaredField("INSTANCE").get(null)
        } catch (_: Throwable) { null }
    }

    private val registrarInstance: Any? by lazy {
        try {
            Class.forName("dejavu.internal.CompositionRegistrar")
                .getDeclaredField("INSTANCE").get(null)
        } catch (_: Throwable) { null }
    }

    private val bindPendingScopeMethod by lazy {
        try {
            observerInstance?.javaClass?.getMethod("bindPendingScope", String::class.java)
        } catch (_: Throwable) { null }
    }

    private val resetMethod by lazy {
        try {
            observerInstance?.javaClass?.getMethod("reset")
        } catch (_: Throwable) { null }
    }

    private val fullResetMethod by lazy {
        try {
            observerInstance?.javaClass?.getMethod("fullReset")
        } catch (_: Throwable) { null }
    }

    private val isAvailableField by lazy {
        try {
            observerInstance?.javaClass?.getDeclaredField("isAvailable")?.apply {
                isAccessible = true
            }
        } catch (_: Throwable) { null }
    }

    private val registrarResetMethod by lazy {
        try {
            registrarInstance?.javaClass?.getMethod("reset")
        } catch (_: Throwable) { null }
    }

    fun bindPendingScope(name: String) {
        try { bindPendingScopeMethod?.invoke(observerInstance, name) } catch (_: Throwable) {}
    }

    fun reset() {
        try { resetMethod?.invoke(observerInstance) } catch (_: Throwable) {}
    }

    fun setAvailable(available: Boolean) {
        try { isAvailableField?.setBoolean(observerInstance, available) } catch (_: Throwable) {}
    }

    fun fullReset() {
        try { fullResetMethod?.invoke(observerInstance) } catch (_: Throwable) {}
    }

    fun resetRegistrar() {
        try { registrarResetMethod?.invoke(registrarInstance) } catch (_: Throwable) {}
    }

    val isAvailable: Boolean
        get() = try {
            isAvailableField?.getBoolean(observerInstance) ?: false
        } catch (_: Throwable) { false }

    fun describeInvalidationCauses(qualifiedName: String): String? {
        val obs = observerInstance ?: return null
        return try {
            obs.javaClass.getMethod("describeInvalidationCauses", String::class.java)
                .invoke(obs, qualifiedName) as? String
        } catch (_: Throwable) { null }
    }

    fun describeStateDependencies(qualifiedName: String): String? {
        val obs = observerInstance ?: return null
        return try {
            obs.javaClass.getMethod("describeStateDependencies", String::class.java)
                .invoke(obs, qualifiedName) as? String
        } catch (_: Throwable) { null }
    }

    /**
     * Try to register the composition observer on a RecomposerInfo.
     * Returns the observer handle, or null if the API is unavailable.
     */
    fun tryRegister(recomposerInfo: Any): Any? {
        val registrar = registrarInstance ?: return null
        return try {
            val observeMethod = recomposerInfo.javaClass.methods.find {
                it.name == "observe" && it.parameterCount == 1
            } ?: return null
            observeMethod.invoke(recomposerInfo, registrar)
        } catch (_: Throwable) { null }
    }

    /**
     * Dispose an observer handle returned by [tryRegister].
     */
    fun disposeHandle(handle: Any) {
        try {
            handle.javaClass.getMethod("dispose").invoke(handle)
        } catch (_: Throwable) {}
    }
}
