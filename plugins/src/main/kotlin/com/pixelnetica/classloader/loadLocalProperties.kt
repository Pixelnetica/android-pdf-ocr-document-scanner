import org.gradle.api.Project

fun Project.loadLocalProperties(): java.util.Properties {
    val localProperties = java.util.Properties()

    try {
        localProperties.load(java.io.FileInputStream(rootProject.file("local.properties")))
    } catch (e: Exception) {
        logger.error("No Local Properties File Found!")
        throw e
    }
    return localProperties
}
