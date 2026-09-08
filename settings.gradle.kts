
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

        // The recommended route. Anonymous, credential-free, declared unconditionally, so
        // this sample builds from a fresh clone with no local.properties at all -- which is
        // exactly what a developer evaluating the SDK starts with.
        maven {
            name = "PixelneticaMaven"
            url = uri("https://maven.pixelnetica.com/")
        }

        // GitHub Packages: still an active source, and sunsetting. Declared only when
        // local.properties supplies all three, because it authenticates every request. Blank
        // counts as absent, so a half-filled file fails here with a missing credential rather
        // than at resolution time with an authentication error.
        val properties = loadLocalPropertiesOrEmpty()
        fun nonBlank(key: String): String? =
            properties.getProperty(key)?.takeIf { it.isNotBlank() }

        val ghUrl = nonBlank("packages_repository")
        val ghUser = nonBlank("packages_user_name")
        val ghPassword = nonBlank("packages_password")
        if (ghUrl != null && ghUser != null && ghPassword != null) {
            maven {
                name = "GitHubPackages"
                url = uri(ghUrl)
                credentials {
                    username = ghUser
                    password = ghPassword
                }
            }
        }
    }
}

// Tolerant by design: absence is the normal state for someone who has just cloned this sample,
// not an error. The throwing version this replaced failed the build at configuration time,
// before resolution was attempted and regardless of whether a credential was needed at all.
fun loadLocalPropertiesOrEmpty(): java.util.Properties {
    val localProperties = java.util.Properties()
    val file = File(rootDir, "local.properties")
    if (file.isFile) {
        file.inputStream().use { localProperties.load(it) }
    }
    return localProperties
}


rootProject.name = "EasyScan"

// App module
include(":easyScan")
