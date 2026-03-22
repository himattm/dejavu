package dejavu.internal

internal data class ParameterChange(
    val parameterName: String,
    val oldValue: String?,
    val newValue: String?,
    val changeType: ChangeType
)

internal enum class ChangeType {
    VALUE_CHANGED, REFERENCE_CHANGED, ADDED, REMOVED
}

internal data class ParamSnapshot(
    val name: String?,
    val valueHash: Int,
    val valueString: String?
)
