plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystoreFile = rootProject.file("keystore.properties")
val keystoreMap = mutableMapOf<String, String>()
if (keystoreFile.exists()) {
    keystoreFile.readLines().forEach { line ->
        val idx = line.indexOf('=')
        if (idx > 0) keystoreMap[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
    }
}
val hasReleaseKey = keystoreMap.containsKey("storeFile")

android {
    namespace = "com.scope.app"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.scope.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.1.2"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystoreMap.getValue("storeFile"))
                storePassword = keystoreMap.getValue("storePassword")
                keyAlias = keystoreMap.getValue("keyAlias")
                keyPassword = keystoreMap.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        debug { versionNameSuffix = "" }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // miuix-nav 以 Java 21 字节码发布，inline 需要一致的 jvm-target
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Miuix：整套 UI 组件基础（Card / Preference / NavigationBar / Dialog / Snackbar）
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-squircle:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-nav:0.9.4")

    // Shizuku（ADB 权限后端）
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // Root（libsu，与 KernelSU / SukiSU 同一套）
    implementation("com.github.topjohnwu.libsu:core:6.0.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
