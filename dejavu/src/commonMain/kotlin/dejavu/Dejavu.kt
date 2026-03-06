package dejavu

/**
 * Main entry point for enabling Dejavu's implicit recomposition tracking.
 *
 * Platform-specific `enable()` methods are available on each target.
 * [disable] is available on all platforms.
 */
public expect object Dejavu {
  /**
   * Removes the tracer and clears all tracked recomposition data.
   */
  public fun disable()
}
