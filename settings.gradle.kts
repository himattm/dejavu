pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  // PREFER_PROJECT allows the Kotlin/Wasm plugin to register its Node.js/Yarn
  // ivy repos at project level (FAIL_ON_PROJECT_REPOS blocks them; KT-68533)
  repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

rootProject.name = "Dejavu"
include(":dejavu", ":demo")
