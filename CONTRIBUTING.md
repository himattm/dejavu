# Contributing to Dejavu

Thank you for your interest in contributing to Dejavu! This guide will help you get started.

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/himattm/dejavu.git
   cd dejavu
   ```

2. **Open in Android Studio** -- use the latest stable version of Android Studio with Kotlin 2.0+ support.

3. **Sync Gradle** -- Android Studio will prompt you to sync on first open. You can also sync manually via `File > Sync Project with Gradle Files`.

## Running Tests

### JVM Unit Tests

```bash
./gradlew :dejavu:testDebugUnitTest
```

These run on your local machine without an emulator and cover core tracker logic and tracer unit tests.

### Instrumented Tests

```bash
./gradlew :demo:connectedDebugAndroidTest
```

These require a running Android emulator or connected device (API 21+). The instrumented test suite covers UI tracking, assertion APIs, stress tests, and error output validation.

## Code Style

- Follow standard [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- No specific formatter is enforced yet. Keep your code consistent with the existing style in the file you are editing.
- Use KDoc for all public API surfaces.

## Submitting PRs

1. **Branch from `main`** -- this is the active development branch.
   ```bash
   git checkout main
   git pull origin main
   git checkout -b your-feature-branch
   ```

2. **Write tests** -- every new feature or bug fix should include tests. Use `createRecompositionTrackingRule<Activity>()` for instrumented tests.

3. **Ensure all tests pass** -- run both JVM and instrumented tests before submitting.

4. **Describe your changes clearly** -- explain what the PR does, why it is needed, and how you tested it.

## API Changes

If your change modifies the public API surface:

1. Run `./gradlew apiDump` to regenerate the API definition file.
2. Commit the updated API file alongside your code changes.
3. CI will run `./gradlew apiCheck` to verify API compatibility.
