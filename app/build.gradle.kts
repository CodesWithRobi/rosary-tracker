plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.rosarytracker"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.rosarytracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Media3 (ExoPlayer + MediaSession)
    implementation("androidx.media3:media3-session:1.11.0")
    implementation("androidx.media3:media3-exoplayer:1.11.0")

    // Media compat (for NotificationCompat.MediaStyle)
    implementation("androidx.media:media:1.7.0")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")

    // Glide for Image Loading
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Coroutines (for suspend functions in Room)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}