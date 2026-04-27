---
name: dejavu-error-triage
description: Triage a single Dejavu recomposition assertion failure. Use when the user pastes a UnexpectedRecompositionsError block, asks why a composable recomposed, mentions "Possible cause" / "Recomposition timeline" / "All tracked composables" output, or wants to understand what a Dejavu error means and apply one fix — without setting up an iterative optimization loop.
---

# Dejavu Error Triage

You have ONE failing Dejavu assertion. This skill walks the error
section-by-section, names the underlying recomposition pattern, and points at
the canonical fix. Apply the fix, re-run the test, done — no iteration loop.

If the goal is iterative optimization (loose `atMost = N` baseline → tighten
to `assertStable()`), use the **`dejavu-perf-loop`** skill instead.
If no Dejavu test exists yet, use **`dejavu-test-writer`** first.

## Reference docs (read first)

- `docs/error-messages.md` — full anatomy of the error block (sections 1–7,
  lines 11–40 for the canonical example) and "Common Failure Patterns" 1–5
  (lines 168–256).
- `docs/causality-analysis.md` — what each "Possible cause" line means,
  including same-value writes and dirty-bit signals.

## Read the error in this order

`docs/error-messages.md` "Reading the Error Quickly" (lines 260–269)
prescribes this; follow it strictly:

1. **Expected vs Actual** — quantify the gap (e.g. expected 0, actual 3).
2. **All tracked composables** — find the `<-- FAILED` marker. If the parent's
   count equals the failed child's count, you have a parent cascade.
3. **Possible cause** — `same-value write` is an immediate bug;
   `Parameter/parent change detected (dirty bits set)` means dirty bits fired.
4. **Recomposition timeline** — `param slots changed: [N]` tells you which
   parameter's dirty bit fired (slot 0 = first parameter).
5. **Composable: name (File.kt:NN)** — jump to source.

Don't guess. Read all five before naming a cause.

## Diagnosis → fix table

Apply the **first matching row**. After applying, re-run the test once.

| # | Signal in the error | Diagnosis | Fix |
|---|---|---|---|
| 1 | `Possible cause:` includes `same-value write` | State written to a value equal to current; reference inequality still triggered recomposition | `data class` for the state holder, OR `mutableStateOf(value, policy = structuralEqualityPolicy())`, OR guard write site with `if (newValue != state.value) state.value = newValue` |
| 2 | `Parameter/parent change detected (dirty bits set)` AND `param slots changed: [N]` AND the param at slot N is a non-data-class | Reference equality on an unstable type; new instance ≠ old instance even with identical fields | Convert param's class to `data class`, or annotate `@Immutable` / `@Stable` |
| 3 | Multiple recompositions on a single interaction; param type broader than what the function actually uses (e.g. `Int` only used in `> 0`) | Type granularity is too coarse | Narrow the parameter type (`Int → Boolean`, `List → Boolean`/`size`) |
| 4 | Same as #3 but the consumer reads a fine-grained value at every parent recomposition AND a derived signal flips less often | Fine-grained state should be coalesced before the boundary | `val coarse by remember { derivedStateOf { fineState.someProperty } }`; pass `coarse` |
| 5 | Parent and many siblings have the same count in `All tracked composables` | Child cascades whenever parent recomposes for unrelated reasons | Hoist the state read into the leaf composable that needs it; or pass `State<T>` and read `.value` in the consumer |
| 6 | Same composable repeats in the timeline once per list item, no `key()` block in the source | Compose can't track item identity across reorderings | `items(list, key = { it.id })`; or `key(it.id) { … }` |
| 7 | Many siblings recompose together because parent re-runs for a coarse reason | Parent state is too broad; siblings inherit the cascade | Move the state into a `CompositionLocal`; or split parent into a stable shell + a state-reading inner composable |
| 8 | `Recomposition timeline` shows `#1`, `#2`, `#3` at increasing timestamps with different `param slots changed` slots | Multiple independent state writes, each invalidating different slots | Batch with `Snapshot.withMutableSnapshot { … }`; consolidate related state into one object; or use `derivedStateOf` to coalesce |
| ? | Pattern doesn't match | Don't guess | Re-read `docs/error-messages.md` "Common Failure Patterns" 1–5 (lines 168–256) and `docs/causality-analysis.md` summary table (lines 251–257) before suggesting a fix |

## Special cases

- **`Warning: testTag '...' could not be mapped to a composable function`** — the
  tag is on a framework-internal node. Move `Modifier.testTag()` to the
  outermost user-defined composable's modifier. (`docs/error-messages.md`
  Pattern 3.)
- **`Actual: composable was never composed or isn't being tracked`** — the tag
  doesn't exist in the composition. For lazy lists, scroll the item into view
  before asserting. (`docs/error-messages.md` Pattern 5.)
- **`IllegalArgumentException` from the assertion itself** — invalid parameter
  combination (e.g. `exactly = 1, atLeast = 1`). Fix the assertion call, not
  the composable.

## Wrap-up

Report back to the user with:

1. **The diagnosis** — quote the specific signal that matched (e.g. "row #2:
   `Parameter/parent change detected` + `param slots changed: [0]` + `class
   CartSummary` is not a `data class`").
2. **The recommended fix** — one specific, applicable change with the file
   and line.
3. **Whether to apply it now** — only edit code if the user explicitly asked
   for a fix, not just a diagnosis. If you're unsure, ask.
4. **Whether iteration is needed** — if the count is much higher than expected
   (e.g. expected `exactly = 0`, actual `7`) and one fix likely won't get all
   the way to the floor, recommend invoking `dejavu-perf-loop` instead of
   guessing the next fix.
