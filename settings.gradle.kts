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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()  // Maven Central primero para Supabase
        maven { url = uri("https://jitpack.io") }  // Para AndroidPdfViewer
    }
}

rootProject.name = "AulaViva"
include(":app")
include(":backend")
