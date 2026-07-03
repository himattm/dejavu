import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

// ============================================================================
// compose-experimental: staging area for experimental-API recomposition coverage.
//
// This module hosts Dejavu recomposition tests for EXPERIMENTAL / newest-Compose
// APIs (currently Compose 1.11's Grid, FlexBox, derivedMediaQuery/mediaQuery,
// Styles, the LinkBuffer composer path, and movableContentOf).
//
// They live here instead of `:dejavu`'s commonTest because `:dejavu`'s commonTest
// is compiled against the full Compose BOM range back to 2024.06 (Compose 1.6) in
// the `compose-compat` CI sweep, where these experimental APIs do not yet exist.
// Putting them in core would break that cross-version sweep, so this module only
// ever builds at the current baseline BOM.
//
// PROMOTION: when an experimental API graduates to stable AND the `:dejavu` BOM
// floor includes it, its composable + SideEffect-backed test is promoted into
// `dejavu/src/commonTest` (the accuracy suite) and removed here. See README.md.
// ============================================================================

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
}

kotlin {
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  androidTarget {
    instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    compilations.all {
      compileTaskProvider.configure {
        compilerOptions {
          jvmTarget.set(JvmTarget.JVM_17)
        }
      }
    }
  }

  jvm {
    compilations.all {
      compileTaskProvider.configure {
        compilerOptions {
          jvmTarget.set(JvmTarget.JVM_17)
        }
      }
    }
  }

  iosSimulatorArm64()
  wasmJs { browser() }

  sourceSets {
    commonMain.dependencies {
      implementation(project(":dejavu"))
      implementation(compose.runtime)
      implementation(compose.ui)
      implementation(compose.foundation)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(project(":dejavu"))
      implementation(compose.runtime)
      implementation(compose.ui)
      implementation(compose.foundation)
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
      implementation(compose.uiTest)
    }

    val jvmTest by getting {
      dependencies {
        implementation(compose.desktop.currentOs)
      }
    }

    val androidInstrumentedTest by getting {
      dependencies {
        implementation(libs.androidx.junit)
        implementation(libs.androidx.espresso.core)
        implementation(libs.androidx.ui.test.junit4)
      }
    }
  }
}

android {
  namespace = "dejavu.experimental"
  compileSdk = 36

  defaultConfig {
    minSdk = 24
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  "debugImplementation"(libs.androidx.ui.test.manifest)
}
