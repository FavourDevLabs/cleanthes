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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Cleanthes"
include(":app")
include(":core:common")
include(":core:security")
include(":core:data:api")
include(":core:data:impl")
include(":core:data:fakes")
include(":core:domain")
include(":core:security:session:api")
include(":core:security:session:impl")
