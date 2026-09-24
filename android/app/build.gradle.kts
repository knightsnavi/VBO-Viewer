plugins {
    id("com.android.application")
}

// Android only installs an update when it is signed by the same key as the copy
// already installed. CI supplies a persistent key from repository secrets; without
// them the build falls back to a throwaway debug key, which installs cleanly on a
// fresh device but can never upgrade an existing install.
val releaseKeystore = System.getenv("SIGNING_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "com.knightsnavi.vboviewer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.knightsnavi.vboviewer"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AGP 8 omits BuildConfig unless asked; MainActivity reads BuildConfig.DEBUG.
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // window insets, so the app clears the status and navigation bars
    implementation("androidx.core:core:1.13.1")
    // WebViewAssetLoader, which serves the app over https instead of file://
    implementation("androidx.webkit:webkit:1.12.1")
}

// The web app is the single source of truth: its files are copied into assets at
// build time rather than duplicated into the Android project.
val webRoot = rootProject.projectDir.parentFile

val copyWebApp by tasks.registering(Copy::class) {
    into(layout.buildDirectory.dir("webassets"))
    from(webRoot) {
        include("index.html", "app.js", "app.css", "vendor/**", "sample/**")
    }
}

android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("webassets"))

tasks.named("preBuild") { dependsOn(copyWebApp) }
