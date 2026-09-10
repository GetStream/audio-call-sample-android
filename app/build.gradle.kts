import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    // AGP 9 has built-in Kotlin support; the kotlin.android plugin is no longer applied.
    // Compose compiler is still a separate Gradle plugin.
    alias(libs.plugins.composeCompiler)

    // For firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "io.getstream.android.sample.audiocall"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.getstream.android.sample.audiocall"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // START - Minimum dependencies for sample app
    // Stream Compose library
    implementation(libs.stream.video.android.ui.compose)
    implementation(libs.stream.chat.client)
    implementation(libs.stream.feed.client)

    // Stream's firebase push library
    implementation(libs.stream.android.push.firebase)
    // Firebase bom, used for push notification
    implementation(platform(libs.firebase.bom))
    // Used by the sample app to store data
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.accompanist.permissions)
    // Required at compile time when subclassing CompatibilityStreamNotificationHandler,
    // whose constructor references android.support.v4.media.session.MediaSessionCompat.
    implementation(libs.androidx.media)
    // END - Minimum dependencies for audio call

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // material-icons-* are no longer transitive from material3 since Compose 1.7; add explicitly.
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}