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
        buildConfigField("boolean", "IS_OFFICIAL", "false")
        buildConfigField("String", "VERSION_NAME", "\"1.0.0\"")
        buildConfigField("String", "BUILD_TYPE_NAME", "\"debug\"")
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
    implementation(libs.androidx.compose.material3.expressive)
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
    implementation(libs.miuix.icons)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.shader)
    implementation(libs.miuix.blur)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
