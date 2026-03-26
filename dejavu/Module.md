# Module dejavu

Implicit recomposition tracking for Compose UI tests.

Dejavu intercepts recomposition events at the runtime level using `CompositionTracer`,
enabling assertions like `assertRecompositions(exactly = 2)` and `assertStable()`
without per-composable setup.

## Getting started

Add the dependency and call `Dejavu.enable()` before your test's `setContent()`.
See the [Getting Started guide](https://dejavu.mmckenna.me/latest/getting-started/) for details.
