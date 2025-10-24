plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.organizer"
    compileSdk = 33  // Mantenemos 33
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packagingOptions {
        resources.excludes += "META-INF/DEPENDENCIES"
    }

    defaultConfig {
        applicationId = "com.example.organizer"
        minSdk = 24
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // ✅ VERSIÓN COMPATIBLE CON SDK 33 - ELIMINA DUPLICADOS
    implementation("androidx.core:core-ktx:1.9.0")

    // Elimina estos duplicados que tienes:
    // implementation("androidx.core:core-ktx:1.6.0")
    // implementation("androidx.core:core-ktx:1.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.json:json:20210307")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.4.0")

    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.http-client:google-http-client-android:1.42.2")
    implementation(libs.gms.drive)
    implementation(libs.gms.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0") // ✅ Versión estable
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.6.1")

    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    implementation("androidx.sqlite:sqlite:2.3.1")
    implementation("androidx.sqlite:sqlite-ktx:2.3.1")

    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // Material Design (versión compatible)
    implementation("com.google.android.material:material:1.8.0")

    // Google Drive API
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
        exclude(module = "httpclient")
    }

    implementation("com.google.apis:google-api-services-drive:v3-rev197-1.25.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    implementation("com.google.http-client:google-http-client-gson:1.43.3") {
        exclude(group = "org.apache.httpcomponents")
    }
}

// ✅ AÑADE ESTO AL FINAL PARA FORZAR VERSIONES COMPATIBLES
configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.9.0")
        force("androidx.core:core:1.9.0")
        force("androidx.appcompat:appcompat:1.6.1")
        // Evita que dependencias traigan versiones incompatibles
        eachDependency {
            when (requested.group) {
                "androidx.core" -> useVersion("1.9.0")
            }
        }
    }
}