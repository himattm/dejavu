# Why Did It Recompose? -- Causality Analysis

When a Dejavu assertion fails, the error message includes a "Possible cause" section and a recomposition timeline. This document explains how Dejavu determines why a composable recomposed, what each piece of information means, and how to use it to fix performance issues.

---

## How Dejavu Detects Causes

Dejavu uses two independent mechanisms to attribute recomposition causes:

### 1. Snapshot State Observer

Dejavu registers a global observer via `Snapshot.registerApplyObserver` to detect state changes:

```
Snapshot.registerApplyObserver { changedObjects, snapshot ->
    // changedObjects: Set<Any> of state objects that were written
    // Dejavu records the count and types of changed state objects
}
```

This captures every `MutableState`, `SnapshotStateList`, `SnapshotStateMap`, and any other `StateObject` that is written during a snapshot transaction. Dejavu records:

- **How many** state objects changed (`stateChanges` count)
- **What types** changed (e.g., `Int`, `String`, `SnapshotStateList`)
- **Whether any writes were same-value** (the new value `==` the old value)

### 2. Dirty Bits from the Composition Tracer

When Compose recomposes a function, the compiler generates a `dirty` bitmask indicating which parameter slots changed. Dejavu's `CompositionTracer` intercepts these values:

- Each parameter occupies 2 bits in the mask
- Bits `00` = unchanged, anything else (`01`, `10`, `11`) = changed
- Slot 0 = first parameter, slot 1 = second, etc.

If any dirty bits are set, Dejavu marks the recomposition as **parameter-driven** (`isParameterDriven = true`). This means the composable recomposed because its parent passed new (or referentially different) parameter values.

---

## The Causality Section in Error Messages

When an assertion fails, the "Possible cause" section combines both signals:

```
  Possible cause:
    1 state change(s) of type Int
    Parameter/parent change detected (dirty bits set)
```

This tells you:
- One `Int` state object changed (likely `mutableIntStateOf`)
- The composable's parameters were also marked dirty by the compiler

Both conditions can be true simultaneously. A state change in a parent can cause the parent to recompose, which then passes new parameter values to the child.

---

## Understanding State Changes

### Simple state change

```
  Possible cause:
    1 state change(s) of type Int
```

One state object of type `Int` was written. This is the most common cause -- a `mutableIntStateOf` or `mutableStateOf<Int>` was updated, and the composable reads it (directly or through a parameter).

### Multiple state types

```
  Possible cause:
    3 state change(s) of type Int, String, SnapshotStateList
```

Three different state objects changed. Dejavu lists the distinct types to help you identify which state sources are involved.

### Same-value writes

```
  Possible cause:
    2 state change(s) of type Product (1 same-value write(s)!)
```

Two state writes occurred, but one of them set the same value that was already there. This is a performance bug: the write triggered a snapshot notification even though nothing logically changed.

**How Dejavu detects same-value writes:**

For `MutableState` objects, Dejavu tracks the value before and after the write:

```
val previousValue = (stateObject as? MutableState<*>)?.value
// ... after write ...
val newValue = (stateObject as? MutableState<*>)?.value
if (previousValue == newValue) {
    // Same-value write detected
}
```

**Common causes of same-value writes:**

- Setting state unconditionally in a callback: `state.value = computeValue()` when the result is the same
- Using a regular class (not `data class`) as a state type, where `equals()` is reference-based
- Re-setting state in a `LaunchedEffect` or `SideEffect` that runs on every recomposition

**Fixes:**

- Guard writes: `if (newValue != state.value) state.value = newValue`
- Use `data class` so `equals()` is structural
- Use `mutableStateOf(value, policy = structuralEqualityPolicy())`

---

## Understanding Parameter-Driven Recompositions

```
  Possible cause:
    Parameter/parent change detected (dirty bits set)
```

This means the composable recomposed because its parent recomposed and passed parameter values that Compose considered "changed." No state read by this composable changed directly -- the recomposition was driven entirely by the parent.

**Common causes:**

1. **Unstable parameter types** -- a non-data-class, lambda, or collection without `@Stable`/`@Immutable` annotation. Compose cannot prove the value is unchanged, so it recomposes.

2. **Overly broad parameter types** -- passing `selectedCount: Int` when the composable only needs `hasSelection: Boolean`. Every increment triggers a recomposition even though the relevant information (is there a selection?) did not change.

3. **Parent cascade** -- the parent recomposed for its own reasons and recreated child parameter values. Even if the logical content is the same, referential inequality causes recomposition.

**Fixes:**

- Use `data class` for parameter types (structural equality)
- Add `@Stable` or `@Immutable` annotations where appropriate
- Narrow parameter types to what the composable actually needs
- Use `derivedStateOf` to convert fine-grained state into coarser signals
- With Compose 1.7+ strong skipping mode, lambdas are auto-memoized -- but non-lambda unstable types still cause issues

---

## Reading the Recomposition Timeline

```
  Recomposition timeline:
    #1 at +0ms — param slots changed: [1] | parent: ProductListScreen
    #2 at +16ms — param slots changed: [1] | parent: ProductListScreen
    #3 at +32ms — param slots changed: [0, 1] | parent: ProductListScreen
```

Each line represents one recomposition event:

### Sequence number (`#N`)

The chronological order of recompositions for this composable since the last reset.

### Relative timestamp (`at +Xms`)

Milliseconds since the first recomposition event for this composable. Useful for:
- Seeing if recompositions are batched (same timestamp) or spread across frames (16ms apart = separate frames)
- Identifying rapid-fire recompositions that suggest a loop or cascade

### Parameter slots changed (`param slots changed: [...]`)

Which parameter slots the Compose compiler marked as dirty:
- `[0]` -- first parameter changed
- `[1]` -- second parameter changed
- `[0, 1]` -- both first and second parameters changed
- `[]` -- no parameter slots dirty (state-driven recomposition)

Map slot numbers to your function signature: `fun MyComposable(first: A, second: B)` where `first` is slot 0 and `second` is slot 1.

### Parent composable (`parent: Name`)

The enclosing composable in the composition stack when the recomposition occurred. This tells you which composable triggered the cascade.

---

## Common Scenarios and Fixes

### Scenario 1: Parent recomposes, child follows

**Timeline:**
```
  #1 at +0ms — param slots changed: [0] | parent: ParentScreen
```

**Cause:** `ParentScreen` recomposed and passed a new value for parameter slot 0.

**Investigation:** Look at what `ParentScreen` passes as the first argument. Is it a new object reference each time? Is it a broader type than needed?

**Fix:** Stabilize the parameter (data class, @Stable), or narrow the type.

### Scenario 2: Same-value write loop

**Cause section:**
```
  3 state change(s) of type Product (3 same-value write(s)!)
```

**Cause:** State is being set to the same value three times. Each write triggers a snapshot notification, causing recomposition even though nothing changed.

**Investigation:** Find where the state is written. Look for unconditional `state.value = ...` in effects, callbacks, or data-mapping code.

**Fix:** Guard the write: `if (newProduct != state.value) state.value = newProduct`

### Scenario 3: Multiple independent state sources

**Timeline:**
```
  #1 at +0ms — param slots changed: [0] | parent: Screen
  #2 at +16ms — param slots changed: [1] | parent: Screen
```

**Cause:** Two different state objects changed in separate frames. The first caused parameter 0 to be dirty, the second caused parameter 1.

**Investigation:** The composable depends on two state sources that update independently. Each update causes a separate recomposition.

**Fix:** If these updates should be atomic, batch them in `Snapshot.withMutableSnapshot { }`. If the composable does not need both, split it into smaller composables that each depend on only one source.

### Scenario 4: Unstable class causes recomposition on unrelated parent change

**Cause section:**
```
  1 state change(s) of type Int
  Parameter/parent change detected (dirty bits set)
```

**Timeline:**
```
  #1 at +0ms — param slots changed: [0] | parent: ProductListScreen
```

**Investigation:** The parent recomposed because an `Int` state changed. The child's parameter slot 0 was marked dirty even though the child's logical input did not change. This happens when the parameter type uses reference equality (regular class, not data class).

**Fix:** Make the parameter type a `data class`, or add `@Stable` annotation with a correct `equals()` implementation.

### Scenario 5: derivedStateOf preventing unnecessary recomposition

**Expected: `assertStable()` passes.**

When `derivedStateOf` correctly coalesces state, the derived value does not change even though the underlying state did. The composable reading the derived value is not recomposed. This is the ideal outcome -- no error message is produced.

If `assertStable()` fails on a composable that reads a `derivedStateOf`, check that:
- The composable reads the derived value, not the raw state
- The derived computation actually returns the same value (not a new object reference each time)

---

## Summary

| Signal | Meaning | Typical Fix |
|--------|---------|-------------|
| State change of type X | A `MutableState<X>` was written | Normal; check if the composable should depend on this state |
| Same-value write | State set to same value | Guard writes, use `data class`, use `structuralEqualityPolicy` |
| Parameter/parent change | Parent passed new parameter values | Stabilize types, narrow parameters, use `derivedStateOf` |
| Multiple timeline entries | Multiple recompositions over time | Batch state updates, split composables, use `derivedStateOf` |
| Same timestamp entries | Multiple recompositions in one frame | Check for cascading invalidation within one composition pass |
