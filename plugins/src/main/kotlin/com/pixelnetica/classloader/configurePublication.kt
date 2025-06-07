import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named

/**
 * Common configurations for subprojects.
 */
fun Project.configurePublication(
    flavor: String,
    configName: String,
    vararg artifactTasks: TaskProvider<*>,
) {
    configure<PublishingExtension> {
        (publications) {
            named<MavenPublication>("aar") {
                // Append dependencies
                pom.withXml {
                    val dependencies = asNode().appendNode("dependencies")
                    configurations.getByName("product${configName.capitalize()}CompileClasspath")
                        .resolvedConfiguration
                        .firstLevelModuleDependencies
                        .forEach {
                            val dependency = dependencies.appendNode("dependency")
                            dependency.appendNode("groupId", it.moduleGroup)
                            dependency.appendNode("artifactId", it.moduleName)
                            dependency.appendNode("version", it.moduleVersion)
                        }
                }

                afterEvaluate {
                    // Add library AAR
                    artifact(tasks.named("bundle${flavor.capitalize()}${configName.capitalize()}Aar"))

                    // If need additional configuration (usually javadoc)
                    artifactTasks.forEach {
                        artifact(it)
                    }
                }
            }
        }
    }
}