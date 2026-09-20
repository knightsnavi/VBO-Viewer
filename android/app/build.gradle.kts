plugins {
    id("com.android.application")
}

android {
    namespace = "com.knightsnavi.vboviewer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.knightsnavi.vboviewer"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the CI build is installable by sideloading.
            signingConfig = signingConfigs.getByName("debug")
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
    // for window insets, so the app clears the status and navigation bars
    implementation("androidx.core:core:1.13.1")
}

// The web app is the single source of truth: its files are copied into assets at
// build time rather than duplicated into the Android project.
val webRoot = rootProject.projectDir.parentFile

val copyWebApp by tasks.registering(Copy::class) {
    into(layout.buildDirectory.dir("webassets"))
    from(webRoot) { include("index.html") }
    from(webRoot) { include("vendor/**") }
    from(webRoot) { include("sample/**") }
}

android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("webassets"))

tasks.named("preBuild") { dependsOn(copyWebApp) }
