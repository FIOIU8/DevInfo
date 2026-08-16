plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.fioiu8.devinfo.feature.main"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
        // IS_OFFICIAL / BUILD_TYPE_NAME 的最终值由 buildTypes 覆盖，
        // 逻辑必须与 app/build.gradle.kts 保持同步（读取相同的环境变量），
        // 否则更新检查会依据过期的硬编码值提前返回。
        buildConfigField("boolean", "IS_OFFICIAL", "false")
        buildConfigField("String", "VERSION_NAME", "\"${System.getenv("VERSION_NAME") ?: "1.0.0"}\"")
        buildConfigField("String", "BUILD_TYPE_NAME", "\"debug\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
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
    implementation("androidx.compose.material:material-icons-extended")

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

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
