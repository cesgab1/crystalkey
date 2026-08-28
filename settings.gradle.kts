pluginManagement {
    repositories {
        // AGP pulls its own Unified Test Platform artifacts from Google's Maven
        // under com.google.testing.platform — leaving com.google.* out of this
        // filter makes the plugin classpath fail to resolve.
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CrystalKey"
include(":app", ":core")
