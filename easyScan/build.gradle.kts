import com.google.protobuf.gradle.*
import com.pixelnetica.classloader.embeddedScanningSdk
import com.pixelnetica.classloader.loadLocalPropertiesOrEmpty
import com.pixelnetica.classloader.projectBuildTools
import com.pixelnetica.classloader.projectCompileSdk
import com.pixelnetica.classloader.projectJavaVersion
import com.pixelnetica.classloader.projectMinSdk
import com.pixelnetica.classloader.projectNdk
import com.pixelnetica.classloader.projectTargetSdk

@Suppress("DSL_SCOPE_VIOLATION") // Remove when fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)     // NOTE: It is need to use 'design' library
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)

}


// Version scheme: versionName is the plain SemVer of the Pixelnetica SDK
// release this app ships (the version catalog's `pixelnetica` value; override
// with -PeasyscanVersionName=X.Y.Z for a demo-only release), versionCode is
// the repository's commit count, and BuildConfig.GIT_HASH records the source
// commit. A shallow clone would yield a too-small versionCode, so it fails
// the build; on a source tree without usable git metadata (for example a ZIP
// download of this sample) the version stamp falls back to neutral values
// with a warning instead of failing the configuration.
val easyscanRepoRoot: File = projectDir.parentFile

fun git(vararg args: String): String = providers.exec {
    workingDir = easyscanRepoRoot
    commandLine("git", *args)
}.standardOutput.asText.get().trim()

val gitAvailable: Boolean = try {
    providers.exec {
        workingDir = easyscanRepoRoot
        commandLine("git", "rev-parse", "--git-dir")
        isIgnoreExitValue = true
    }.result.get().exitValue == 0
} catch (e: Exception) {
    false // git itself cannot be launched
}

val gitStamp: Pair<Int, String> = if (gitAvailable) {
    if (git("rev-parse", "--is-shallow-repository") == "true") {
        throw GradleException("Shallow git clone detected for easyscan; run 'git fetch --unshallow' in $easyscanRepoRoot")
    }
    Pair(git("rev-list", "HEAD", "--count").toInt(), git("rev-parse", "--short", "HEAD"))
} else {
    logger.warn("Git metadata unavailable for easyscan; using fallback version stamp")
    Pair(1, "nogit")
}
val easyscanVersionCode: Int = gitStamp.first
val easyscanGitHash: String = gitStamp.second

val easyscanVersionName: String = (findProperty("easyscanVersionName") as String?)?.also {
    require(it.matches(Regex("""\d+\.\d+\.\d+"""))) {
        "easyscanVersionName must be numeric MAJOR.MINOR.PATCH, got '$it'"
    }
} ?: libs.versions.pixelnetica.get()

tasks.register("printVersionInfo") {
    description = "Prints the stamped version values as stable key=value lines"
    val name = easyscanVersionName
    val code = easyscanVersionCode
    val hash = easyscanGitHash
    doLast {
        println("versionName=$name")
        println("versionCode=$code")
        println("gitHash=$hash")
    }
}

android {
    namespace = "com.pixelnetica.easyscan"

    compileSdk = projectCompileSdk
    buildToolsVersion = projectBuildTools
    ndkVersion = projectNdk

    defaultConfig {
        applicationId = "com.pixelnetica.easyscan"
        minSdk = projectMinSdk
        targetSdk = projectTargetSdk
        versionCode = easyscanVersionCode
        versionName = easyscanVersionName
        buildConfigField("String", "GIT_HASH", "\"$easyscanGitHash\"")

        ndk.debugSymbolLevel = "FULL"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }


    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }

    buildFeatures {
        aidl = false
        buildConfig = true
        compose = true
        dataBinding = true  // NOTE: It is need to use 'design' library
        renderScript = false
        viewBinding = false
        shaders = false
    }

    compileOptions {
        // Using Java 11
        sourceCompatibility = projectJavaVersion
        targetCompatibility = projectJavaVersion
    }

    packaging {
        // Workaround strange error 'More than one file was found with OS independent path'
        resources.pickFirsts.add("*.so")

        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    protobuf {
        protoc {
            artifact = "com.google.protobuf:protoc:${libs.versions.protobufJavalite.get()}"
        }
        generateProtoTasks {
            ofSourceSet("main")
            all().forEach { task ->
                task.builtins {
                    id("java") {
                        option("lite")
                    }
                }
            }
        }
    }

    buildTypes {
        debug {
            kotlin.target.compilerOptions.freeCompilerArgs.add("-Xdebug")
            manifestPlaceholders += mapOf("enableCrashReporting" to "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")

            manifestPlaceholders += mapOf("enableCrashReporting" to "true")

            packaging {
                // disable coroutines debug
                resources.excludes.add("DebugProbesKt.bin")
            }

        }
    }


    if (embeddedScanningSdk) {
        flavorDimensions.add("stage")
        productFlavors {
            // Link libraries as project to debug ensemble
            create("develop") {
                dimension = "stage"
            }
            // Link libraries from artifactory server
            create("product") {
                dimension = "stage"

                buildFeatures {
                    // Data binding is used in libraries
                    dataBinding = true
                }
            }
        }
    }
}

configurations.all {
    exclude(module = "commons-logging")
    exclude(module = "httpclient")
    exclude(module = "xpp3")
}

dependencies {

    if (embeddedScanningSdk) {
        // Implementations for different flavors
        val productImplementation by configurations
        val developImplementation by configurations

        // Pixelnetica scanning SDK comes from sibling project or from online repository
        developImplementation(project(":camera"))
        productImplementation(libs.pixelnetica.camera)

        developImplementation(project(":design"))
        productImplementation(libs.pixelnetica.design)

        developImplementation(project(":scanning"))
        productImplementation(libs.pixelnetica.scanning)

        developImplementation(project(":support"))
        productImplementation(libs.pixelnetica.support)
        developImplementation(libs.androidx.exifinterface)
    } else {
        // Standalone build
        implementation(libs.pixelnetica.support)
        implementation(libs.pixelnetica.scanning)
        implementation(libs.pixelnetica.design)
        implementation(libs.pixelnetica.camera)
    }


    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.hilt.android)
    implementation(libs.google.gson)
    implementation(libs.google.protobuf.javalite)
    implementation(libs.kotlin.reflect)
    debugImplementation(libs.androidx.compose.ui.tooling)

    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.test.junit)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

