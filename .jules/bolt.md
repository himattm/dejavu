## 2024-05-14 - Use data classes for Compose parameters
**Learning:** Classes passed to Compose functions should be data classes so that Jetpack Compose compares them structurally via equals(). Otherwise, identity equality causes unnecessary recompositions when the parent recomposes.
**Action:** Always use data classes for objects holding view state passed as composable parameters.
