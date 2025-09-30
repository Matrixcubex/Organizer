plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")  // Añade este plugin

}

android {
    namespace = "com.example.organizer"
    compileSdk = 33  // Actualizado a 33 para compatibilidad con dependencias
    buildFeatures {
        viewBinding = true // Habilita View Binding
    }
    packagingOptions {
        resources.excludes += "META-INF/DEPENDENCIES"
    }

    defaultConfig {
        applicationId = "com.example.organizer"
        minSdk = 24  // Mantenemos Nougat (API 24)
        targetSdk = 30  // Puedes dejarlo en 30 para evitar cambios de comportamiento
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
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("com.google.http-client:google-http-client-android:1.42.2")
    implementation(libs.gms.drive)   // ← nuevo
    implementation(libs.gms.auth)
    implementation(libs.google.api.client.android)   // ← REST auth
    implementation(libs.google.api.services.drive)   // ← Drive v3
    // Básicas de AndroidX (necesarias para casi todo)
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.6.1") // Para ArrayAdapter
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")

    // Navigation Component (para los fragments)
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")


    // ViewModel y LiveData (solo si los usas)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")

    // SQLite directo (sin Room)
    implementation("androidx.sqlite:sqlite:2.0.1")
    implementation("androidx.sqlite:sqlite-ktx:2.0.1")

    // Para manejo de contactos
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // Para mapas (opcional)
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // Material Design (para pickers)
    implementation("com.google.android.material:material:1.9.0")
    // Google Drive API
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
        exclude(module = "httpclient")
    }

    implementation("com.google.apis:google-api-services-drive:v3-rev197-1.25.0") {
        exclude(group = "org.apache.httpcomponents")
        implementation("com.google.http-client:google-http-client-gson:1.43.3") {
            exclude(group = "org.apache.httpcomponents")
        }

    }
}
