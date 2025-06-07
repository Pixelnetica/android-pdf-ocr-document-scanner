import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradleplugin.android)
    implementation(libs.gradleplugin.kotlin)

    // Workaround for version catalog working inside precompiled scripts
    // Issue - https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

private val pluginsJavaVersion: JavaVersion = JavaVersion.toVersion(libs.versions.pluginsJava.get())

java {
    sourceCompatibility = pluginsJavaVersion
    targetCompatibility = pluginsJavaVersion
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(pluginsJavaVersion.toString()))
}

gradlePlugin {
    // Add fake plugin, if you don't have any
    plugins.register("class-loader-plugin") {
        id = "class-loader-plugin"
        implementationClass = "ClassLoaderPlugin"
    }
    // Or provide your implemented plugins
}
