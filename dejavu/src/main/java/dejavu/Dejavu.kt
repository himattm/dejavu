package dejavu

import android.app.Application
import dejavu.internal.Runtime

/**
 * Main entry point for enabling Dejavu's implicit recomposition tracking.
 *
 * Dejavu uses [androidx.compose.runtime.CompositionTracer] to intercept
 * recomposition events without requiring per-composable modifiers.
 */
public object Dejavu {
  /**
   * Installs the CompositionTracer and starts tracking recompositions.
   * Call in Application.onCreate() or before setContent().
   *
   * @param app Application instance for composition owner discovery
   * @param logToLogcat When true, recomposition events are streamed to Logcat
   *   under the "Dejavu" tag. Useful for giving AI agents or external tools
   *   real-time visibility into composition changes and UI state.
   */
  public fun enable(
    app: Application,
    logToLogcat: Boolean = false
  ): Unit = Runtime.enable(app, logToLogcat)

  /**
   * Removes the tracer and clears all tracked recomposition data.
   */
  public fun disable(): Unit = Runtime.disable()
}
