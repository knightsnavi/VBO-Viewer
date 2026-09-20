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
        versionCode = 1
        versionName = "1.0"
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
