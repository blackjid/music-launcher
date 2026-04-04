import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.blackjid.musiclauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blackjid.musiclauncher"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["redirectSchemeName"] = "com.blackjid.musiclauncher"
        manifestPlaceholders["redirectHostName"] = "callback"

        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${localProperties.getProperty("SPOTIFY_CLIENT_ID", "")}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"${localProperties.getProperty("SPOTIFY_CLIENT_SECRET", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    // Spotify SDK
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))
    implementation(files("libs/spotify-auth-release-2.1.0.aar"))
    implementation(libs.gson)

    // Encrypted storage for OAuth tokens
    implementation(libs.security.crypto)
    implementation(libs.datastore.preferences)

    debugImplementation(libs.compose.ui.tooling)
}
