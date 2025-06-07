import com.google.protobuf.gradle.*
import com.pixelnetica.classloader.embeddedScanningSdk
import com.pixelnetica.classloader.projectCompileSdk
import com.pixelnetica.classloader.projectJavaVersion
import com.pixelnetica.classloader.projectMinSdk
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

android {
    namespace = "com.pixelnetica.easyscan"
    compileSdk = projectCompileSdk

    defaultConfig {
        applicationId = "com.pixelnetica.easyscan"
        minSdk = projectMinSdk
        targetSdk = projectTargetSdk
        versionCode = 81
        versionName = "3.0.$versionCode"

        ndk.debugSymbolLevel = "FULL"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "GIT_HASH", "\"${getLastCommitHash()}\"")
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

        // Try to insert debug symbols (currently it doesn't work)
        jniLibs.keepDebugSymbols.add("*.so")

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
            kotlinOptions.freeCompilerArgs += "-Xdebug"
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
            packaging {
                // disable coroutines debug
                resources.excludes.add("DebugProbesKt.bin")
            }
        }
    }
    buildToolsVersion = "35.0.0"
    ndkVersion = "28.0.12433566 rc1"

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
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.fragment.ktx)
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

    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.test.junit)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}