# Dejavu

Implicit recomposition tracking for Compose UI tests. KMP library targeting Android, Desktop (JVM), iOS, and WasmJs.

## Verification Requirements

**Always run UI tests when validating changes.** This is a UI testing framework — unit tests alone are not sufficient. The SideEffect accuracy tests in `commonTest` verify that the tracer's recomposition counts match ground-truth `SideEffect` counters through actual Compose UI rendering.

Minimum verification after any code change:

```bash
./gradlew -q --console=plain :dejavu:jvmTest                    # Desktop JVM (unit + compose UI tests)
./gradlew -q --console=plain :dejavu:testDebugUnitTest           # Android unit tests
./gradlew -q --console=plain :dejavu:iosSimulatorArm64Test       # iOS compose UI tests on simulator
./gradlew -q --console=plain :dejavu:wasmJsBrowserTest           # Wasm compose UI tests in headless browser
./gradlew -q --console=plain apiCheck                            # API compat (all targets)
```

`jvmTest`, `iosSimulatorArm64Test`, and `wasmJsBrowserTest` all run the compose UI integration tests (SideEffect accuracy, recomposition counting) from `commonTest`. Android excludes compose UI tests from `testDebugUnitTest` (they require Robolectric); Android compose coverage comes from instrumented tests in the demo app.

## Project Structure

- `commonMain` — core tracer, data classes, query APIs, test assertions, expect declarations
- `androidMain` — Android runtime (Activity lifecycle, Choreographer, snapshot observer, tag mapping via Group tree)
- `jvmMain` — Desktop JVM actuals (Composer.setTracer, ThreadLocal, stdout logging)
- `iosMain` — iOS actuals (NSDate, NSLog, simple value holder for PlatformThreadLocal)
- `wasmJsMain` — WasmJs actuals (JS Date.now(), console.log/warn)
- `commonTest` — unit tests + compose UI integration tests (SideEffect accuracy)

## Key Architecture

- `DejavuTracer` implements `CompositionTracer` — intercepts every `traceEventStart`/`traceEventEnd`
- First composition of a key → tracked but not counted as recomposition. Subsequent → counted.
- Tag mapping (testTag → function name) is Android-only via Group tree walking. Other platforms use function-name tracking directly.
- Locking uses `kotlinx-atomicfu` `SynchronizedObject` (not `kotlin.synchronized` which is JVM-only)
- `@kotlin.concurrent.Volatile` in common/native code (not `@Volatile` which is `kotlin.jvm.Volatile`)

## Reset Semantics

- `reset()` — full clear of all state including composition history. Use between independent tests.
- `resetCounts()` — clears recomposition counts but preserves composition history. Use mid-test when a live composition is still running.
- `ComposeTestRule.resetRecompositionCounts()` calls `resetCounts()` (mid-test use case).

## Gradle

Always run with `-q --console=plain`.

## Bundled Claude skills

This repo ships two skills under `.claude/skills/` for AI agents working with Dejavu:

- `dejavu-test-writer` — author Compose UI recomposition tests using Dejavu's APIs.
- `dejavu-perf-loop` — closed-loop optimization of a composable's recomposition behavior, using Dejavu as the validator. Invokes `dejavu-test-writer` to establish the baseline test.

Both skills point at the canonical docs in `docs/` and the canonical test patterns in `dejavu/src/commonTest/kotlin/dejavu/*PatternTest.kt` rather than duplicating them. They auto-load for sessions opened in this repo; downstream consumers can copy them into their own `.claude/skills/`.
