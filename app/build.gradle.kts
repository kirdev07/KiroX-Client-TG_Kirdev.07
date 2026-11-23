import java.util.Properties

val kiroxProperties = Properties().apply {
    val file = rootProject.file("kirox.properties")
    if (file.exists()) file.inputStream().use(::load)
}
/** First non-blank value among [names] from kirox.properties or the environment. */
fun secret(vararg names: String): String =
    names.firstNotNullOfOrNull { name -> (kiroxProperties.getProperty(name) ?: System.getenv(name))?.trim()?.takeIf { it.isNotEmpty() } }.orEmpty()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firebase push is configured only when app/google-services.json is present (not committed).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    // releaseCheck (package com.kirox.app.check) is a local R8 check build without Firebase:
    // google-services.json has no client for it, so its config task is skipped.
    tasks.matching { it.name == "processReleaseCheckGoogleServices" || it.name == "processV3previewGoogleServices" || it.name == "processQadcGoogleServices" || it.name == "processQareleaseGoogleServices" }.configureEach { enabled = false }
}

android {
    // Native libraries (TDLib, call engine) are ~50 MB raw and ~20 MB compressed: compress them in the
    // APK so downloads and updates are about half the size; Android unpacks them once on install.
    packaging { jniLibs { useLegacyPackaging = true } }
    namespace = "com.kirox.app"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.kirox.app"
        // Android 10 – 17, 64-bit ARM only (owner decision for 1.0).
        minSdk = 29
        targetSdk = 37
        versionCode = 9
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("int", "TELEGRAM_API_ID", secret("KIROX_API_ID", "kirox.apiId").toIntOrNull()?.toString() ?: "0")
        buildConfigField("boolean", "TELEGRAM_USE_TEST_DC", "false")
        // kiroX v3 preview: the new design on demo data, as its own app (see buildTypes.v3preview).
        buildConfigField("boolean", "KX_V3_DEMO", "false")
        manifestPlaceholders["appLabel"] = "kiroX"
        buildConfigField("String", "TELEGRAM_API_HASH", "\"${secret("KIROX_API_HASH", "kirox.apiHash").replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        // TDLib and the call engine ship arm64 only.
        ndk {
            abiFilters += "arm64-v8a"
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_static", "-DANDROID_ARM_NEON=TRUE")
                // Only the call engine: libyuv is linked into it statically, its own shared build (libyuv.so) is unused.
                targets += "kiroxvoip"
                abiFilters += "arm64-v8a"
            }
        }
    }

    ndkVersion = "27.2.12479018"
    externalNativeBuild {
        cmake {
            path = file("../voip/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        // Release key lives outside the repository; its path and passwords come from kirox.properties.
        val storePath = secret("KIROX_RELEASE_STORE_FILE")
        if (storePath.isNotEmpty() && file(storePath).exists()) create("release") {
            storeFile = file(storePath)
            storePassword = secret("KIROX_RELEASE_STORE_PASSWORD")
            keyAlias = secret("KIROX_RELEASE_KEY_ALIAS")
            keyPassword = secret("KIROX_RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        // TELEGRAM_USE_TEST_DC stays false (defaultConfig) everywhere except the separate `qadc` build below.
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // R8-optimized build signed with the local debug key and installed next to the normal app,
        // to check minification on a device without touching the real install or its session.
        create("releaseCheck") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".check"
            matchingFallbacks += "release"
        }
        // The new kiroX v3 design on demo data only: installed next to the real app, never signs in.
        create("v3preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".v3"
            matchingFallbacks += "debug"
            buildConfigField("boolean", "KX_V3_DEMO", "true")
            manifestPlaceholders["appLabel"] = "kiroX v3"
        }
        // Telegram test servers (test DC) for team QA: its own app id, data and TDLib databases; test accounts
        // only (+99966XYYYY), never the real Telegram. AGP forbids build types named "test…", hence `qadc`. src/qadc adds the KxSim broadcast harness (qa/testdc.md).
        create("qadc") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".testdc"
            matchingFallbacks += "debug"
            buildConfigField("boolean", "TELEGRAM_USE_TEST_DC", "true")
            manifestPlaceholders["appLabel"] = "kiroX test"
        }
        // Release-candidate check for the team: the exact release build (R8, shrunk resources, not debuggable) but
        // signed with the local debug key and installed as com.kirox.app.qarel next to everything else; never signs in.
        // Install only through `kxbuild.py installrel` (Xiaomi 79ed61c9). No Firebase (no client for this id).
        create("qarelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".qarel"
            isDebuggable = false
            matchingFallbacks += "release"
            manifestPlaceholders["appLabel"] = "kiroX rel-test"
        }
    }

    compileOptions {
        // java.time and other Java 8+ library APIs on Android 7 (API < 26).
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Generated TDLib bindings (5 MB) exceed lint's file size limit; the code is not ours to fix.
        disable += "LintError"
        // Plain-text report, turned into CI annotations by the workflow.
        textReport = true
        textOutput = file("build/reports/lint-results-debug.txt")
        abortOnError = false
    }

    // Robolectric renders resources (themes, fonts) for the screenshot test.
    testOptions { unitTests.isIncludeAndroidResources = true }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}


dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.google.libphonenumber)
    implementation(libs.zxing.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Video, GIF, video note and music playback (Apache-2.0).
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    // Background playback of voice notes and music with a media notification.
    implementation(libs.androidx.media3.session)
    // Animated TGS stickers (Lottie, Apache-2.0).
    implementation(libs.lottie.compose)
    // Video note recording: CameraX capture, Media3 square crop (both Apache-2.0).
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.annotation)
    // 7z archives in the file viewer and the file check (xz: LZMA / LZMA2 inside 7z).
    implementation(libs.commons.compress)
    implementation(libs.xz)
    // Installs src/main/baseline-prof.txt so the start-up code is compiled ahead of time.
    implementation(libs.androidx.profileinstaller)
    // Photo orientation in the photo editor.
    implementation(libs.androidx.exifinterface)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit)
    // Screenshots of the main screens on the JVM (ScreenshotTest, run by the screenshots workflow).
    testImplementation(composeBom)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
}
