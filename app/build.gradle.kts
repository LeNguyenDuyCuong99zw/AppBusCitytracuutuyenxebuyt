plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.map.buscity"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.map.buscity"
        minSdk = 26
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.3")
// version mới nhất

    // --- Jetpack Compose cơ bản ---
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")

    // --- Icons (bộ biểu tượng dùng trong Login/Register/Home) ---
    implementation("androidx.compose.material:material-icons-core:1.6.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")

    // --- Coil để load ảnh online (dùng trong HomeActivity) ---
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- Các thư viện cơ bản của AndroidX ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    // Use explicit version to avoid BOM resolution issues in some environments
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Room
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // MapLibre: add stable, pinned SDK version to avoid breaking changes from newer releases.
    // Use the official MapLibre Android SDK artifact coordinate (android-sdk).
    // If you want a newer/stable release, replace the version below (e.g. android-v11/android-v12 tags).
    implementation("org.maplibre.gl:android-sdk:11.11.0")
    // MapLibre plugins for annotations
    implementation("org.maplibre.gl:android-plugin-annotation-v9:2.0.2")
    // OkHttp for networking used to call OSRM routing service
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // DataStore for simple persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}