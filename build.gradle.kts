import com.pixelnetica.classloader.projectJavaVersion

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    dependencies {
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath(libs.protobuf.plugin)
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // Remove when will be fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt.gradle) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.protobuf) apply false

    // Load custom variables
    id("class-loader-plugin")
}

subprojects {
    apply(plugin = "class-loader-plugin")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xcontext-receivers")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(projectJavaVersion.toString()))
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
