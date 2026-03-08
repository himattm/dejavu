plugins {
  id("com.android.library")
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "me.mmckenna.dejavu"
version = "0.1.2"

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
}

// Set Kotlin JVM target to 17
kotlin {
  explicitApi()
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)

  // Only sign when GPG credentials are available (skips signing for CI mavenLocal publishes)
  if (project.hasProperty("signing.keyId") || System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null) {
    signAllPublications()
  }

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

dependencies {
  // Compose BOM for version alignment (overridable via -PcomposeBomVersion=...)
  val composeBomVersion = project.findProperty("composeBomVersion") as? String
  val composeBom = if (composeBomVersion != null) {
    platform("androidx.compose:compose-bom:$composeBomVersion")
  } else {
    platform(libs.androidx.compose.bom)
  }

  // JVM unit tests
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(composeBom)
  testImplementation("androidx.compose.runtime:runtime")

  compileOnly(composeBom)
  compileOnly("androidx.compose.runtime:runtime")
  compileOnly(libs.androidx.ui.tooling)
  implementation(composeBom)
  implementation(libs.kotlinx.coroutines.android)
  implementation("androidx.compose.ui:ui-tooling-data")
  // Needed for SemanticsNodeInteraction assertion extensions and test helpers
  compileOnly("androidx.compose.ui:ui-test")
  compileOnly("androidx.compose.ui:ui-test-junit4")
  // Pinned to avoid Gradle resolution conflict with BOM-managed transitive version; compileOnly, never shipped
  compileOnly("androidx.activity:activity-compose:1.7.0")
  // Pinned to avoid Gradle resolution conflict with BOM-managed transitive version; compileOnly, never shipped
  compileOnly("androidx.test.ext:junit:1.1.5")
}
