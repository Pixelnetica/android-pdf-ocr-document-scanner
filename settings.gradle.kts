
pluginManagement {
    repositories {
        // Common variables and configurations
        includeBuild("plugins")
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // PIXELNETICA REPOSITORY
        maven {
            val properties = loadLocalProperties()
            url = uri(checkNotNull(properties["packages_repository"]) {
                "Cannot find key 'packages_repository' in file local.properties"
            })
            credentials {
                username = properties.getProperty("packages_user_name")
                password = properties.getProperty("packages_password")
            }
        }
    }
}

fun loadLocalProperties(): java.util.Properties {
    val localProperties = java.util.Properties()

    try {
        localProperties.load(java.io.FileInputStream(File(rootDir, "local.properties")))
    } catch (e: Exception) {
        logger.error("File 'local.properties' is not found!")
        throw e
    }
    return localProperties
}


rootProject.name = "EasyScan"

// App module
include(":easyScan")
