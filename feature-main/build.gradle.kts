plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.fioiu8.devinfo.feature.main"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
        // VERSION_NAME 只在 defaultConfig 声明（两个 buildType 都不覆盖它）。
        // 必须与 app/build.gradle.kts 读取同一个环境变量，否则更新检查会依据
        // 过期的硬编码版本号提前返回。
        buildConfigField("String", "VERSION_NAME", "\"${System.getenv("VERSION_NAME") ?: "1.0.0"}\"")
    }

    buildTypes {
        release {
            // 与 app 模块一致：CI 通过 SIGNATURE_TYPE 环境变量区分官方构建与本地构建
            val isOfficial = System.getenv("SIGNATURE_TYPE") == "release"
            buildConfigField("boolean", "IS_OFFICIAL", isOfficial.toString())
            buildConfigField("String", "BUILD_TYPE_NAME", "\"${if (isOfficial) "official" else "dev"}\"")
        }
        debug {
            buildConfigField("boolean", "IS_OFFICIAL", "false")
            buildConfigField("String", "BUILD_TYPE_NAME", "\"debug\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// Ensure R.jar is generated before Kotlin compilation
afterEvaluate {
    tasks.findByName("compileDebugKotlin")?.dependsOn("generateDebugRFile", "compileDebugLibraryResources")
    tasks.findByName("compileReleaseKotlin")?.dependsOn("generateReleaseRFile", "compileReleaseLibraryResources")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":ui"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Activity
    implementation(libs.androidx.activity.compose)

    // Miuix
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.nav)
    implementation(libs.kotlinx.serialization.json)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
