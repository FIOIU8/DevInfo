pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // OAID/AAID：JitPack 提供库本体，华为/荣耀仓库提供厂商广告标识 SDK。
        maven("https://jitpack.io") {
            content { includeGroup("com.github.gzu-liyujiang") }
        }
        maven("https://developer.huawei.com/repo")
        maven("https://developer.hihonor.com/repo")
    }
}

rootProject.name = "DevInfo"
include(":app")
include(":core")
include(":data")
include(":ui")
include(":feature-main")
