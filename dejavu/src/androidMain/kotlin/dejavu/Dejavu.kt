package dejavu

import android.app.Application
import dejavu.internal.Runtime

/**
 * Main entry point for enabling Dejavu's implicit recomposition tracking.
 *
 * Dejavu uses [androidx.compose.runtime.CompositionTracer] to intercept
 * recomposition events without requiring per-composable modifiers.
 */
public actual object Dejavu {
  /**
   * Installs the CompositionTracer and starts tracking recompositions.
   * Call in Application.onCreate() or before setContent().
   *
   * @param app Application instance for composition owner discovery
   * @param logToLogcat When true, recomposition events logged to Logcat
   */
  public fun enable(
    app: Application,
    logToLogcat: Boolean = false
  ): Unit = Runtime.enable(app, logToLogcat)

  /**
   * Removes the tracer and clears all tracked recomposition data.
   */
  public actual fun disable(): Unit = Runtime.disable()
}
