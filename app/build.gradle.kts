import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    id("com.github.ben-manes.versions") version "0.52.0"
}

val signatureType = System.getenv("SIGNATURE_TYPE")
val isCI = System.getenv("CI")?.toBoolean() == true

gradle.taskGraph.whenReady {
    val releaseTaskRequested =
        allTasks.any { task ->
            task.name.contains("release", ignoreCase = true)
        }
    if (isCI && releaseTaskRequested && signatureType != "release") {
        throw GradleException(
            "CI release build requires SIGNATURE_TYPE=release. " +
                "Debug signing is not allowed in CI environment.",
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

        // 无 CI 环境变量时用秒级时间戳（上限 21 亿内）作本地默认，
        // 保证本地构建可直接覆盖安装历史正式版而不被判为降级
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull()
            ?: (System.currentTimeMillis() / 1000L).toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "IS_OFFICIAL", "false")
        buildConfigField("String", "BUILD_TYPE_NAME", "\"dev\"")

        // D1: 仅保留主流 ABI，减少 APK 体积
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        // D2: 仅保留项目支持的语言资源
        resourceConfigurations += listOf("en", "zh", "ja")
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
                    isCI -> {
                        // CI 环境中未指定 SIGNATURE_TYPE（如 verify job），使用 debug
                        signingConfigs.getByName("debug")
                    }
                    else -> {
                        // 本地开发允许降级到 debug 签名
                        logger.warn("⚠️  Local release build using debug signing (missing SIGNATURE_TYPE=release)")
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
