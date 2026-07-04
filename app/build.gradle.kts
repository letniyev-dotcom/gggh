plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.letify.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.letify.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 156
        versionName = "r156-home-revert"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            // Keystore lives in app/ alongside this build file. Password/alias
            // are intentionally committed because this is a dev keystore used
            // purely so the release APK is installable straight off CI — it
            // is NOT the production Play Store signing key.
            storeFile = file("letify-release.jks")
            storePassword = "letify"
            keyAlias = "letify"
            keyPassword = "letify"
        }
    }

    buildTypes {
        release {
            // R8 full-mode code shrinking + optimization. Safe here: the app
            // has no reflection / JSON deserialization, and every persisted
            // enum round-trips through an explicit `key` string (Tab("home"),
            // TransitionStyle("push"), …) rather than enum.name, so obfuscation
            // can't break state restore. Resource shrinking is safe too — the
            // Solar icons live in assets/icons/ (not res/), which the resource
            // shrinker never touches.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        buildConfig = false
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.runtime:runtime")

    // Coil + SVG decoder (Coil 2.x — matches imports in SolarIcon.kt)
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Real frosted-glass background blur for the navbar. On Android
    // 12+ uses the platform RenderEffect blur; on older it gracefully
    // falls back to the bare tint. Pinned to a Compose 1.6 compatible
    // release.
    implementation("dev.chrisbanes.haze:haze:0.7.3")
    implementation("io.coil-kt:coil-svg:2.6.0")
    // AndroidSVG — declared explicitly (coil-svg only exposes it
    // transitively at runtime). SolarIconLoader.decodeInto rasterises the
    // navbar/icon SVGs with this directly on a worker thread, off the main
    // thread, so the cold-launch icon prewarm can't deadlock the UI.
    implementation("com.caverock:androidsvg-aar:1.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Baseline Profile installer. AGP merges app/src/main/baseline-prof.txt
    // into the release APK's baseline profile (assets/dexopt/baseline.prof);
    // profileinstaller applies it on API 24–30 (and assists 31+), so the hot
    // Compose animation/transition code is AOT-compiled on first run instead
    // of being interpreted/JIT'd mid-slide — kills the first-transition jank.
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
        )
    }
}
