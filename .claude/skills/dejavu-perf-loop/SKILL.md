---
name: dejavu-perf-loop
description: Optimize a Compose composable's recomposition behavior using Dejavu as a closed-loop validator. Use when the user asks to reduce recompositions, find why a composable recomposes, lock in a recomposition optimization, hit a recomposition budget, fix a header/banner cascade, investigate "Possible cause" / "Recomposition timeline" output, or apply derivedStateOf / @Immutable / data class fixes driven by failing Dejavu assertions.
---

# Dejavu Perf Loop

Iteratively optimize the recomposition behavior of a Compose composable using
Dejavu as the validator. The loop is: write/extend a test → run → read the
failure → apply ONE fix → re-run → tighten the assertion → repeat until the
target hits its theoretical recomposition minimum. The final test is the
regression guard.

## Reference docs (read before fixing)

- `docs/error-messages.md` — full error structure and "Common Failure Patterns"
  (#1–5). Read sections 1–7, lines 11–40 for the canonical example.
- `docs/causality-analysis.md` — what "Possible cause" actually means; summary
  table at the bottom (lines 251–257).
- `docs/examples.md` — Examples 2 (Boolean narrowing, lines 89–124), 3 (data
  class, lines 132–171), 8 (`derivedStateOf`, lines 462–493).

## Companion skill

This skill assumes a Dejavu test exists or can be written. To author or extend
the test, **invoke the `dejavu-test-writer` skill** — read its `SKILL.md` at
`.claude/skills/dejavu-test-writer/SKILL.md` and follow its workflow, then
return here with a working test that has a baseline assertion.

## Workflow

### 1. Pick a target

Identify ONE composable + ONE user interaction. Examples:
- "ProductHeader after refresh button click"
- "CartBanner after item selection"
- "DerivedBanner after select-all"

Narrow scope makes diagnosis tractable. Don't try to optimize a whole screen
at once.

### 2. Establish a baseline test

Either extend an existing test or write a new one (use `dejavu-test-writer`).
Start with a LOOSE upper bound and at least one stable sibling:

```kotlin
@Test
fun productHeader_recompositionBudget() {
    composeTestRule.onNodeWithTag("refresh_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("product_header").assertRecompositions(atMost = 10)
    composeTestRule.onNodeWithTag("static_label").assertStable()   // scoping guard
}
```

The baseline's job is not to pass — it's to capture a real number you can shrink.

### 3. Run the test, observe the count

Run the appropriate gradle command (see `dejavu-test-writer` "Run the test").
- If `atMost = 10` passes, lower it progressively (`5`, `3`, `2`, `1`) and re-run
  until it FAILS. If `atMost = N` is the first to fail and `atMost = M` (M > N)
  was the previous pass, the actual count is in `[N + 1, M]`. To pin the exact
  number, narrow the bracket with further probes (or, on Android, read it
  directly via `composeTestRule.getRecompositionCount(tag)`).
- When it fails, capture the **full** error block. Every section in
  `docs/error-messages.md` is a diagnostic input.

### 4. Compute the theoretical minimum

For the interaction you triggered:
- 0 state changes that the target reads → `assertStable()` should hold
- 1 state change the target reads → `1` is the floor
- N independent state changes the target reads → `N` is the floor (unless coalesced)

If actual > theoretical, proceed to Step 5. Otherwise tighten and commit.

### 5. Apply the decision tree below — ONE fix at a time

Read the Dejavu error and match against the table. Apply exactly one fix per
iteration. Multiple fixes at once make it impossible to attribute the count
change.

### 6. Tighten the assertion after each successful fix

- Actual dropped to 0 → replace `atMost` with `assertStable()`
- Actual dropped to known N → replace `atMost` with `assertRecompositions(exactly = N)`
- Still bounded but lower → lower `atMost`, leave a TODO to tighten further

**Never raise `atMost` to make a test pass.** That's a regression, not an
optimization.

### 7. Repeat until convergence

Convergence = actual equals theoretical minimum AND assertion is `exactly` /
`assertStable()`.

### 8. Commit the locked-in test as a regression guard

Add a one-line comment naming the cookbook entry the test guards. Don't commit
unless the user asks.

## Decision tree: error pattern → fix

Apply the **first matching row**. Re-run the test after every fix.

| # | Error signal in Dejavu output | Fix to apply | Reference |
|---|---|---|---|
| 1 | `Possible cause:` includes `same-value write` | Convert state holder to `data class` for structural equality; OR `mutableStateOf(value, policy = structuralEqualityPolicy())`; OR guard at write site: `if (newValue != state.value) state.value = newValue` | `error-messages.md` Pattern 2; `causality-analysis.md` |
| 2 | `Possible cause:` says `Parameter/parent change detected (dirty bits set)` AND timeline shows `param slots changed: [N]` AND the param's type is a non-data-class | Convert the param's class to `data class`, or annotate `@Immutable` / `@Stable` | `examples.md` Example 3 (lines 132–171) |
| 3 | Multiple recompositions on a single interaction; param type is broader than what the function actually uses (e.g. `Int` only used in `> 0`, list only used for `.isNotEmpty()`) | Narrow the parameter type (e.g. `Int → Boolean`, `List → Boolean` or `List → size`) | `examples.md` Example 2 (lines 89–124) |
| 4 | Same as #3 but the consumer reads a fine-grained value at every parent recomposition AND the derived signal flips less often | `val coarse by remember { derivedStateOf { fineState.someProperty } }`; pass `coarse` to the child | `examples.md` Example 8 (lines 462–493) |
| 5 | `All tracked composables` shows the parent and many siblings with the same count; child cascades whenever parent recomposes for unrelated reasons | Hoist the state read into the leaf composable that needs it; OR pass `State<T>` and read `.value` in the consumer | `error-messages.md` Pattern 1 |
| 6 | Same composable appears in the timeline once per list item with no `key()` block | Add `key = { it.id }` to `items()`; or wrap loop bodies in `key(it.id) { … }` | — |
| 7 | Many siblings recompose together because parent re-runs for a coarse reason | Push the changing state into its own `CompositionLocal` so only readers re-run; OR split the parent into a stable shell + a state-reading inner composable | `examples.md` Example 7 |
| 8 | Timeline shows `#1`, `#2`, `#3` at increasing timestamps with different `param slots changed` slots, suggesting independent state writes | Batch with `Snapshot.withMutableSnapshot { … }`; OR consolidate related state into one object; OR use `derivedStateOf` to coalesce | `error-messages.md` Pattern 4 |
| ? | Unknown / ambiguous error | Re-read `error-messages.md` "Reading the Error Quickly" (lines 260–269) — read in order: Expected vs Actual → cascade marker → Possible cause → timeline → source location. Don't guess. | `error-messages.md` |

## How to read the error fast

1. **Expected vs Actual** — the gap.
2. **All tracked composables** — find the `<-- FAILED` marker. If the parent has
   the same count as the failed child, you have a cascade (rows #5 / #7).
3. **Possible cause** — `same-value write` (row #1) is an immediate bug;
   `Parameter/parent change` is the start of the dirty-bit investigation.
4. **Recomposition timeline** — `param slots changed: [N]` tells you which
   parameter's dirty bit fired. Map slot index to function-signature position
   (slot 0 = first param).
5. **Composable source location** — `Composable: pkg.Name (File.kt:NN)` jumps
   you to the function.

## Wrap-up

The final, locked-in test should:

- Use `assertStable()` or `assertRecompositions(exactly = N)` — never `atMost`.
- Have at least one sibling `assertStable()` to prove scoping.
- Carry a one-line comment naming the cookbook entry it guards
  (e.g. `// Locks in row #3 (Boolean narrowing) for ProductHeader`).

When reporting back to the user, surface:

- Before / after recomposition counts.
- The specific cookbook row applied (and the fix's `examples.md` line range).
- The exact gradle command that proves the regression guard works.
