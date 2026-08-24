import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    id("com.github.ben-manes.versions") version "0.52.0"
}

val signatureType = System.getenv("SIGNATURE_TYPE")
val configuredVersionName = System.getenv("VERSION_NAME") ?: "1.0.0"

fun stableVersionCode(versionName: String): Int {
    val match = Regex("^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+].*)?$").find(versionName) ?: return 1
    val major = match.groupValues[1].toLongOrNull() ?: return 1
    val minor = match.groupValues[2].toLongOrNull() ?: 0L
    val patch = match.groupValues[3].toLongOrNull() ?: 0L
    return (major * 10_000L + minor * 100L + patch)
        .coerceIn(1L, Int.MAX_VALUE.toLong())
        .toInt()
}

gradle.taskGraph.whenReady {
    val releaseTaskRequested =
        allTasks.any { task ->
            task.name.contains("release", ignoreCase = true)
        }
    if (releaseTaskRequested && signatureType != "release") {
        throw GradleException(
            "Release build requires SIGNATURE_TYPE=release. " +
                "Debug signing is not allowed for release artifacts.",
        )
    }
}

android {
    namespace = "com.fioiu8.devinfo"
    compileSdk = 37

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.fioiu8.devinfo"
        minSdk = 33
        targetSdk = 37

        // CI 可显式传入版本号；本地构建从版本名稳定推导，避免同一源码每次产生不同 APK。
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull()?.takeIf { it > 0 }
            ?: stableVersionCode(configuredVersionName)
        versionName = configuredVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "IS_OFFICIAL", "false")
        buildConfigField("String", "BUILD_TYPE_NAME", "\"dev\"")

        // D1: 仅保留主流 ABI，减少 APK 体积
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // D2: 仅保留项目支持的语言资源
    androidResources {
        localeFilters += listOf("en", "zh", "ja")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig =
                when {
                    signatureType == "release" -> {
                        signingConfigs.getByName("release")
                    }
                    else -> {
                        // taskGraph 校验会拒绝实际的 Release 任务；此配置仅用于完成 Gradle 配置阶段。
                        signingConfigs.getByName("debug")
                    }
                }

            val isOfficial = signatureType == "release"
            buildConfigField("boolean", "IS_OFFICIAL", isOfficial.toString())
            buildConfigField("String", "BUILD_TYPE_NAME", "\"${if (isOfficial) "official" else "dev"}\"")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            buildConfigField("boolean", "IS_OFFICIAL", "false")
            buildConfigField("String", "BUILD_TYPE_NAME", "\"debug\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// 放在 android 块外面，根级别
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // Feature modules
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":ui"))
    implementation(project(":feature-main"))

    // App-specific dependencies (not needed by feature modules)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Compose runtime (needed for setContent)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.datastore.preferences)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
