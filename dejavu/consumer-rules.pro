# Keep Dejavu's public API surface discoverable by test code.
# This rule ships to consumers via consumerProguardFiles so tests
# that reference Dejavu classes (e.g., DejavuComposeTestRule) are not
# stripped by R8/ProGuard during minified builds.
-keep class dejavu.Dejavu { *; }
-keep class dejavu.DejavuComposeTestRule { *; }
-keep class dejavu.SemanticNodeInteractionsKt { *; }
-keep class dejavu.DejavuComposeTestRuleKt { *; }
