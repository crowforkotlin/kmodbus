pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    versionCatalogs {
        create("ktor") {
            from(files("gradle/ktor.versions.toml"))
        }

        create("app") {
            from(files("gradle/app.versions.toml"))
        }
    }

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KModbus"
include(":app")
include(":lib_modbus")
