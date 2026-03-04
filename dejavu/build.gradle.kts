plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
  id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "me.mmckenna.dejavu"
version = "0.1.1"

kotlin {
  explicitApi()

  androidTarget {
    compilations.all {
      compileTaskProvider.configure {
        compilerOptions {
          jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
      }
    }
    publishLibraryVariants("release")
  }

  jvm {
    compilations.all {
      compileTaskProvider.configure {
        compilerOptions {
          jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
      }
    }
  }

  iosArm64()
  iosSimulatorArm64()
  iosX64()

  wasmJs { browser() }

  sourceSets {
    val iosMain by creating {
      dependsOn(commonMain.get())
    }
    val iosArm64Main by getting { dependsOn(iosMain) }
    val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    val iosX64Main by getting { dependsOn(iosMain) }

    commonMain.dependencies {
      compileOnly(compose.runtime)
      compileOnly(compose.ui)
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
      compileOnly(compose.uiTest)
      implementation(libs.kotlinx.atomicfu)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(compose.runtime)
      implementation(compose.ui)
      implementation(compose.foundation)
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
      implementation(compose.uiTest)
    }

    androidMain.dependencies {
      compileOnly(libs.androidx.ui.tooling)
      implementation(libs.kotlinx.coroutines.android)
      implementation("androidx.compose.ui:ui-tooling-data")
      // Needed for DejavuComposeTestRule (Android-specific JUnit4 rule)
      compileOnly("androidx.compose.ui:ui-test-junit4")
      // Pinned to avoid Gradle resolution conflict with BOM-managed transitive version; compileOnly, never shipped
      compileOnly("androidx.activity:activity-compose:1.7.0")
      // Pinned to avoid Gradle resolution conflict with BOM-managed transitive version; compileOnly, never shipped
      compileOnly("androidx.test.ext:junit:1.1.5")
    }

    val jvmTest by getting {
      dependencies {
        implementation(compose.runtime)
        implementation(compose.desktop.currentOs)
      }
    }

    val androidUnitTest by getting {
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
        implementation(libs.robolectric)
        implementation(compose.runtime)
      }
    }
  }
}

android {
  namespace = "dejavu"
  compileSdk = 36
  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")
  }
  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }
  lint {
    warningsAsErrors = true
    abortOnError = true
    // Suppress upgrade-nag and version-catalog migration checks
    disable += setOf(
      "GradleDependency",
      "NewerVersionAvailable",
      "AndroidGradlePluginVersion",
      "UseTomlInstead",
    )
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        // Compose UI tests use runComposeUiTest which requires Robolectric on Android;
        // these tests run on JVM desktop instead. Android UI tests run as instrumented tests.
        it.exclude("dejavu/DejavuComposeUiTest.class")
        it.exclude("dejavu/SideEffectAccuracyTest.class")
      }
    }
  }
}

// Compose BOM for version alignment (overridable via -PcomposeBomVersion=...)
dependencies {
  val composeBomVersion = project.findProperty("composeBomVersion") as? String
  val composeBom = if (composeBomVersion != null) {
    platform("androidx.compose:compose-bom:$composeBomVersion")
  } else {
    platform(libs.androidx.compose.bom)
  }
  "androidMainCompileOnly"(composeBom)
  "androidMainImplementation"(composeBom)
  "androidUnitTestImplementation"(composeBom)
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()

  coordinates("me.mmckenna.dejavu", "dejavu", version.toString())

  pom {
    name.set("Dejavu")
    description.set("Implicit recomposition tracking for Jetpack Compose UI tests")
    url.set("https://github.com/himattm/dejavu")
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("himattm")
        name.set("Matt McKenna")
        url.set("https://blog.mmckenna.me")
      }
    }
    scm {
      url.set("https://github.com/himattm/dejavu")
      connection.set("scm:git:git://github.com/himattm/dejavu.git")
      developerConnection.set("scm:git:ssh://github.com/himattm/dejavu.git")
    }
  }
}
