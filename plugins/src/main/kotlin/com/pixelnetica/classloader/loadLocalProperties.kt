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

/**
 * The tolerant twin of [loadLocalProperties]: returns an empty [java.util.Properties] when
 * `local.properties` is absent or unreadable, instead of throwing.
 *
 * This sample resolves the Pixelnetica SDK from an anonymous Maven repository that needs no
 * credentials, so a fresh clone has nothing to put in `local.properties` and may not have the
 * file at all. An eager read on the configuration path would abort such a build before
 * dependency resolution was even attempted -- failing for a missing file rather than for
 * anything the build actually needed. Callers that genuinely require a value still fail, but
 * they fail on the missing value rather than on the missing file.
 *
 * [loadLocalProperties] is kept for callers that legitimately cannot proceed without it.
 */
fun Project.loadLocalPropertiesOrEmpty(): java.util.Properties {
    val file = rootProject.file("local.properties")
    if (!file.isFile) {
        logger.info("local.properties not found; continuing without it.")
        return java.util.Properties()
    }
    return java.util.Properties().apply { file.inputStream().use { load(it) } }
}
