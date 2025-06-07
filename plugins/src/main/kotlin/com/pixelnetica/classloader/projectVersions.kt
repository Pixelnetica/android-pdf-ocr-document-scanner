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

val Project.embeddedScanningSdk: Boolean
    get() = libs.versions.embeddedScanningSdk.isPresent && libs.versions.embeddedScanningSdk.get().toInt() > 0