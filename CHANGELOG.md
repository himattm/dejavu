# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
