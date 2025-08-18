package com.pixelnetica.classloader

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.gradle.accessors.dm.LibrariesForLibs

private val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()


val Project.projectJavaVersion: JavaVersion
    get() = JavaVersion.toVersion(libs.versions.java.get().toInt())


val Project.projectCompileSdk: Int
    get() = libs.versions.compileSdk.get().toInt()


val Project.projectTargetSdk: Int
    get() = libs.versions.targetSdk.get().toInt()


val Project.projectMinSdk: Int
    get() = libs.versions.minSdk.get().toInt()


val Project.projectBuildTools: String
    get() = libs.versions.buildTools.get()


val Project.projectNdk: String
    get() = libs.versions.ndk.get()


val Project.projectCmake: String
    get() = libs.versions.cmake.get()


val Project.embeddedScanningSdk: Boolean
    get() = try {
        loadLocalProperties()
            .getProperty("embedded_scanning_sdk")
            .trim(' ', '"') // Skip quotes
            .toBoolean()
    } catch (ex: Exception) {
        // Work OK if "local.properties" does not exist.
        false
    }

