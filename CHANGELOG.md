# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Bump Kotlin from 2.2.20 to 2.3.20
- Bump AGP from 8.13.2 to 9.1.0
- Bump Gradle wrapper from 8.13 to 9.4.1
- Bump Compose Multiplatform from 1.10.1 to 1.10.3
- Bump Compose BOM from 2026.01.01 to 2026.03.00
- Bump Activity Compose from 1.7.0 to 1.13.0
- Bump atomicfu from 0.27.0 to 0.32.0
- Bump Robolectric from 4.14.1 to 4.16.1
- Bump core-ktx from 1.17.0 to 1.18.0
- Update compatibility tables and CI matrix for 2026.03.01 BOM baseline

### Added
- `runRecompositionTrackingUiTest` and `setTrackedContent` public APIs for KMP test setup (JVM, iOS, WasmJs)
- Dokka API documentation generation

## [0.3.0] - 2026-03-22

### Added
- Kotlin Multiplatform support — JVM Desktop, iOS, and WasmJs targets (#13)
- 210 cross-platform tests reaching 98% coverage parity with Android
- Cross-platform tag mapping and per-instance recomposition tracking
- KMP demo targets (Desktop, iOS, Wasm) with shared demo module

### Changed
- CI matrix expanded with cross-platform regression tests and hardened verification
- Consolidated fingerprint baseline capture into `resetCounts()` for deterministic test setup

### Fixed
- Timing-sensitive per-tag counting for multi-instance composables
- Thread safety issues for concurrent access across platforms
- iOS/WasmJs test failures from platform-specific actuals

## [0.2.0] - 2026-03-16

### Added
- CompositionObserver integration — automatically tracks per-scope invalidation causes and state dependencies on Compose runtime 1.7+ (#27)
  - Enriches `assertStable()` / `assertRecompositions()` error messages with invalidation cause details, value progressions, and state dependency sets
  - Auto-detected at `Dejavu.enable()` — no opt-in required
  - Backward-compatible: observer classes in separate source set, core compiles against Compose BOMs 2024.06+
  - Reentrancy guard in `stateValue()` prevents StackOverflowError from DerivedState reads
  - Uses IdentityHashMap instead of identityHashCode to avoid hash collisions

### Fixed
- Per-tag recomposition tracking: false positives from identity-based multi-instance detection, null-fallback to function-level counts, and runtime-internal objects drifting fingerprint hashes (#28)
- Flaky per-tag recomposition detection in CI

### Changed
- CI publish workflow auto-updates docs version references at deploy time

## [0.1.2] - 2026-03-08

### Changed
- Improved logcat output — unified TAG to "Dejavu", single summary lines, cleaner parent notation, consistent log levels (#23)

### Fixed
- testTag mapping for older Compose versions using value-based InspectableValue (#16)
- Hardcoded framework filter replaced with dynamic simpleNameIndex resolution (#20)

## [0.1.1] - 2026-03-03

### Changed
- Logcat logging now emits per-instance `RECOMPOSE-TAG` lines for composables with a testTag, matching Layout Inspector's per-instance recomposition counts
- Composables without a testTag still get the per-function `RECOMPOSE` line with a `[no testTag — per-function aggregate]` caveat
- No more dual logging — each composable gets exactly one log format

## [0.1.0] - 2026-02-23

### Added
- Implicit recomposition tracking via `CompositionTracer` — no per-composable modifiers needed
- `Dejavu.enable()` / `Dejavu.disable()` API for runtime control
- `DejavuComposeTestRule` for automated test setup with auto-reset
- Semantic node assertions: `assertRecompositions(exactly, atLeast, atMost)` and `assertStable()`
- Recomposition cause tracking with `Snapshot.registerApplyObserver`
- Rich assertion error messages with source location, timeline, and causality info
- Tag-to-composable mapping via CompositionData tree walking
- 23 demo screens covering common Compose patterns
- 160 tests (150 instrumented + 10 JVM unit)
