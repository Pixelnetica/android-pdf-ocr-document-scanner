package com.pixelnetica.classloader

import org.gradle.api.Project

fun Project.loadLocalProperties(): java.util.Properties {
    try {
        return java.util.Properties().apply {
            load(java.io.FileInputStream(rootProject.file("local.properties")))
        }
    } catch (e: Exception) {
        logger.error("The file \"local.properties\" wasn't found!")
        throw e
    }
}
