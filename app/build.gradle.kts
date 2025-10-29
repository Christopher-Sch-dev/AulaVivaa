plugins {
    alias(libs.plugins.kotlin.android)
    id("com.android.application")
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    // KSP para Room (genera código en tiempo de compilación)
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

android {
    namespace = "cl.duocuc.aulaviva"
    compileSdk = 34

    defaultConfig {
        applicationId = "cl.duocuc.aulaviva"
        minSdk = 24
        targetSdk = 34
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
        viewBinding = true
    }
}

dependencies {

    // AndroidX y Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity)
    
    // Activity KTX (necesario para viewModels())
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Firebase BoM (gestiona versiones automáticamente)
    implementation(platform(libs.firebase.bom))

    // Firebase products (sin versión, BoM se encarga)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.analytics)

    // Room Database para persistencia local (funciona offline)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Corrutinas para operaciones asíncronas con Room
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Google Generative AI (Gemini) - Integración de IA real
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
