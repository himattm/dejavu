## 2024-05-03 - Data Classes ensure structural equality

**Learning:** Unstable class definitions that are missing `data` (like `class CartSummary`) pass around object references, which means `equals()` depends on the reference pointing to the same place, and not structural content, causing Recompose in Jetpack Compose when any parent composable function creates new object instances with identical property values.

**Action:** Look for instances of `class` arguments into composable functions being used as State and check if upgrading to `data class` optimizes and drops excessive recomposition logic for components that are fed non-structurally equal object references holding the same internal state.
