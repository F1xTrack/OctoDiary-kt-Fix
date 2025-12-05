pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlin/kotlin") }
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlin/kotlin") }
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
}

rootProject.name = "OctoDiary"
include(":app")
 