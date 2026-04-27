---
name: dejavu-test-writer
description: Author Compose UI recomposition tests using the Dejavu library. Use when adding a new test for a Compose composable, retrofitting recomposition assertions onto an existing UI test, deciding between Android instrumented vs KMP common tests, or when the user mentions Dejavu, recomposition counts, assertRecompositions, assertStable, or createRecompositionTrackingRule.
---

# Dejavu Test Writer

Dejavu turns Compose recomposition behavior into JUnit/kotlin.test assertions. This
skill picks the right setup, points at canonical examples, and gives you the API
surface in one screen so you can write a correct test on the first try.

This skill teaches you how to **write a test**. It is a companion to
`dejavu-perf-loop`, which uses these tests as a closed-loop validator to optimize
recomposition behavior. If a count exceeds your assertion, do **not** loosen the
assertion — invoke `dejavu-perf-loop` instead.

## Reference docs (read first)

Read these before generating any test code:

- `docs/getting-started.md` — minimal Android setup
- `docs/examples.md` — eight canonical patterns, including LazyColumn,
  AnimatedVisibility, deep nesting, CompositionLocal, and `derivedStateOf`
- `docs/error-messages.md` — failure output anatomy (used by the perf-loop skill)
- `README.md` — KMP setup section + Known Limitations / Known Gaps

## Canonical examples to copy from

| Style | File |
|---|---|
| Android JUnit4 | `demo/src/androidTest/java/demo/app/AssertionApiTest.kt` |
| KMP common test | `dejavu/src/commonTest/kotlin/dejavu/AssertionApiPatternTest.kt` |
| Smallest KMP example | `dejavu/src/commonTest/kotlin/dejavu/DejavuComposeUiTest.kt` |
| Tagged composable | `demo-shared/src/commonMain/kotlin/demo/app/ui/Counter.kt` |
| Optimization patterns (more) | `dejavu/src/commonTest/kotlin/dejavu/*PatternTest.kt` |

## API at a glance

### Android (JUnit4) setup

```kotlin
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule

@get:Rule
val composeTestRule = createRecompositionTrackingRule<MyActivity>()
// or createRecompositionTrackingRule() for plain ComponentActivity host
```

`createRecompositionTrackingRule` wraps `createAndroidComposeRule`, enables Dejavu
before each test, resets counts, and disables on teardown. Drop-in replacement.

### KMP (commonTest / Desktop / iOS / Wasm) setup

```kotlin
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import dejavu.assertStable
import dejavu.runRecompositionTrackingUiTest
import dejavu.setTrackedContent
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MyTest {
    @Test
    fun myComposable_isStable() = runRecompositionTrackingUiTest {
        setTrackedContent { MyComposable() }
        waitForIdle()
        onNodeWithTag("my_tag").assertStable()
    }
}
```

`runRecompositionTrackingUiTest` is the KMP equivalent of the Android rule. It
handles tracer setup, inspection-table seeding, state reset, and teardown.
`setTrackedContent` is the tracked equivalent of `setContent` — required for tag
mapping on non-Android platforms.

### Assertions

```kotlin
onNodeWithTag(tag).assertRecompositions(exactly = N)
onNodeWithTag(tag).assertRecompositions(atLeast = N)
onNodeWithTag(tag).assertRecompositions(atMost = N)
onNodeWithTag(tag).assertRecompositions(atLeast = N, atMost = M)
onNodeWithTag(tag).assertStable()                         // alias for exactly = 0
```

Validation rules (`SemanticNodeInteractions.kt:33-50`): exactly one of `exactly` or
`atLeast`/`atMost`; `exactly` cannot combine with the range params; all bounds must
be `>= 0`; `atLeast <= atMost`. Violations throw `IllegalArgumentException`.

### Mid-test reset (Android)

```kotlin
composeTestRule.resetRecompositionCounts()
```

Clears recomposition counts but preserves composition history. Use between setup
and the actual interaction under test (`docs/examples.md` Tips section).

### Programmatic read (Android only)

```kotlin
val n: Int = composeTestRule.getRecompositionCount("my_tag")
```

## Workflow

### 1. Decide where the test lives

- **Android-only feature, needs an Activity / hardware** →
  `demo/src/androidTest/...` (or your project's instrumented test source set)
- **Cross-platform composable** → `dejavu/src/commonTest/...` (or your KMP
  module's `commonTest`)
- See `CLAUDE.md` "Project Structure" for module roles.

### 2. Tag every assertion target

Add `Modifier.testTag("my_tag")` to the composable you want to assert on. The tag
must be on a user-defined composable's modifier — framework-internal nodes cannot
be mapped (`docs/error-messages.md` Pattern 3). Convention: snake_case strings.

### 3. Pick the assertion mode

- `assertStable()` — "this should never recompose" (clearest intent)
- `assertRecompositions(exactly = N)` — known count, regression guard
- `assertRecompositions(atMost = N)` — recomposition budget; tighten this over
  time using the perf-loop skill
- `assertRecompositions(atLeast = N)` — sanity check that an interaction did
  trigger recomposition

### 4. Drive UI → waitForIdle → assert

```kotlin
composeTestRule.onNodeWithTag("inc_button").performClick()
composeTestRule.waitForIdle()                              // ALWAYS
composeTestRule.onNodeWithTag("counter_value").assertRecompositions(exactly = 1)
```

Skip `waitForIdle()` and counts become racy. Same applies to `performTextInput`
and any other interaction.

### 5. Reset between phases of multi-step tests

When the test has setup actions plus a measured interaction, reset between them
so the assertion only counts the interaction under test:

```kotlin
composeTestRule.onNodeWithTag("select_0_btn").performClick()
composeTestRule.waitForIdle()
composeTestRule.resetRecompositionCounts()                 // forget setup

composeTestRule.onNodeWithTag("select_all_btn").performClick()
composeTestRule.waitForIdle()
composeTestRule.onNodeWithTag("derived_banner").assertStable()
```

### 6. Run the test

This repo's convention (`CLAUDE.md` "Gradle"):

```bash
./gradlew -q --console=plain :dejavu:jvmTest                    # Desktop JVM (KMP common)
./gradlew -q --console=plain :dejavu:testDebugUnitTest           # Android unit tests
./gradlew -q --console=plain :dejavu:iosSimulatorArm64Test       # iOS sim (KMP common)
./gradlew -q --console=plain :dejavu:wasmJsBrowserTest           # Wasm headless browser
./gradlew -q --console=plain :demo:connectedDebugAndroidTest     # Android instrumented
./gradlew -q --console=plain apiCheck                            # API compat
```

Always pass `-q --console=plain`.

## Common gotchas

- **Off-screen lazy items** — scroll into view before asserting; off-screen items have no composition (`README.md` Known Limitations).
- **`exactly` + range mutual exclusion** — `exactly` cannot combine with `atLeast`/`atMost`; throws `IllegalArgumentException`.
- **Negative bounds** — all bounds must be `>= 0`.
- **`assertStable()` vs `exactly = 0`** — identical; prefer `assertStable()` for intent.
- **`LazyVerticalGrid` on iOS/Wasm** — upstream Compose crash; use `LazyColumn`/`LazyRow` (`README.md` Known Gaps).
- **Wasm assertion-message swallowing** — don't introspect `AssertionError.message` on Wasm; pass/fail still works.
- **`testTag` placement** — put the tag on the outermost user-defined composable's modifier, not buried inside framework primitives.
- **Activity-owned clock** — `createAndroidComposeRule` uses the real Recomposer; use plain `createComposeRule()` for clock control.

## Wrap-up

After writing or editing a test:

1. Run the matching gradle command above and fix failures.
2. **If the count exceeds your assertion, do NOT loosen it** — invoke the
   `dejavu-perf-loop` skill to diagnose and fix the underlying recomposition
   cause, then come back with a tightened assertion.
3. Leave at least one `assertStable()` on a sibling composable that should not
   be affected — it locks in the scoping guarantee and catches setup mistakes.
