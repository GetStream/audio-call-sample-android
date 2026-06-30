plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)

    // For firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "io.getstream.android.sample.audiocall"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.getstream.android.sample.audiocall"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // START - Minimum dependencies for sample app
    // Stream Compose library
    implementation(libs.stream.video.android.ui.compose)

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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}