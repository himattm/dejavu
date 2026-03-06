package dejavu.internal

internal data class RecomposeCause(
  val stateChanges: Int = 0,
  val types: List<String> = emptyList(),
  val sameValueWrites: Int = 0,
  val isParameterDriven: Boolean = false
)
