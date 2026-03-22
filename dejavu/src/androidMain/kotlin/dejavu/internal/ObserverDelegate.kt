package dejavu.internal

/**
 * Interface for composition observer features. Main code calls this
 * directly — no reflection needed. The real implementation lives in
 * src/observerAndroid/ and is loaded via Class.forName when the Compose
 * observer API is available.
 */
internal interface ObserverDelegate {
    val isAvailable: Boolean
    fun bindPendingScope(name: String)
    fun describeInvalidationCauses(qualifiedName: String): String?
    fun describeStateDependencies(qualifiedName: String): String?
    fun reset()
    fun fullReset()
    fun resetRegistrar()
    fun tryRegister(recomposerInfo: Any): Any?
    fun disposeHandle(handle: Any)
}

/** Default no-op used when the Compose observer API is unavailable. */
internal object NoOpObserver : ObserverDelegate {
    override val isAvailable: Boolean = false
    override fun bindPendingScope(name: String) {}
    override fun describeInvalidationCauses(qualifiedName: String): String? = null
    override fun describeStateDependencies(qualifiedName: String): String? = null
    override fun reset() {}
    override fun fullReset() {}
    override fun resetRegistrar() {}
    override fun tryRegister(recomposerInfo: Any): Any? = null
    override fun disposeHandle(handle: Any) {}
}
