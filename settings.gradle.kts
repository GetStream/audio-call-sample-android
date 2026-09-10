pluginManagement {
    repositories {
        mavenLocal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
            content {
                includeGroupByRegex("io\\.getstream.*")
            }
            mavenContent {
                snapshotsOnly()
            }
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
            content {
                includeGroupByRegex("io\\.getstream.*")
            }
            mavenContent {
                snapshotsOnly()
            }
        }
    }
}

rootProject.name = "AudioCallSample"
include(":app")
 