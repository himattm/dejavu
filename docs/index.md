# Dejavu

*Wait... didn't we just compose this?*

[![CI](https://github.com/himattm/dejavu/actions/workflows/ci.yml/badge.svg)](https://github.com/himattm/dejavu/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/me.mmckenna.dejavu/dejavu)](https://central.sonatype.com/artifact/me.mmckenna.dejavu/dejavu)
[![Compose](https://img.shields.io/badge/Compose-1.6.x–1.10.x-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/develop/ui/compose)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**[GitHub Repository](https://github.com/himattm/dejavu)**

**Guard your Compose UI efficiency. Catch recomposition regressions before your users.**

## The Problem

Compose's recomposition behavior is an implicit contract — composables should recompose when their inputs change and stay stable otherwise. But that contract breaks silently, and today's options for catching it are limited:

- **Layout Inspector** — manual, requires a running app, can't automate, can't run in CI
- **Manual tracking code** — `SideEffect` counters, `LaunchedEffect` logging, wrapper composables; invasive, doesn't scale, and ships in your production code
- Neither gives you a **testable, automatable contract** you can enforce on every PR

## What Dejavu Does

Dejavu is a test-only library that turns recomposition behavior into assertions. Tag your composables with standard `Modifier.testTag()`, write expectations against recomposition counts, and get structured diagnostics when something changes — whether from a teammate, a library upgrade, an AI agent rewriting your UI code, or a refactor that silently destabilizes a lambda.

- **Zero production code changes** — just `Modifier.testTag()`
- **One-line test setup** — `createRecompositionTrackingRule()`
- **Rich diagnostics** — source location, recomposition timeline, parameter diffs, causality analysis
- **Per-instance tracking** — multiple instances of the same composable get independent counters

## Next Steps

- [Getting Started](getting-started.md) — add the dependency and write your first test
- [Use Cases](use-cases.md) — locking in UI efficiency, AI agent guardrails, and CI enforcement
- [Examples](examples.md) — test patterns for common scenarios
- [API Reference](api-reference.md) — all assertions and utilities
- [How It Works](how-it-works.md) — internals, compatibility, and limitations

## Using Claude Code?

Install the bundled `dejavu-test-writer` and `dejavu-perf-loop` skills globally:

```
/plugin marketplace add himattm/dejavu
/plugin install dejavu@dejavu
```

See [Use Cases → Give AI Agents a Recomposition Signal](use-cases.md#give-ai-agents-a-recomposition-signal) for what each skill does.
