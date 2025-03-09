plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.maayn.mealmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maayn.mealmate"
        minSdk = 29
        targetSdk = 35
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
        dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)


    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Firebase (using BOM for version alignment)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database)

    // Google Play Services Auth
    implementation(libs.play.services.auth)

    // Retrofit (TheMealDB API)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Lottie (Splash Animation)
    implementation(libs.lottie)

    // ExoPlayer (Video Embedding)
    implementation(libs.exoplayer)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coil (Image Loading)
    implementation(libs.coil)

    // Glide (Image Loading)
    implementation(libs.glide)
    kapt(libs.compiler)

    // Gson
    implementation(libs.gson)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines Play Services
    implementation(libs.kotlinx.coroutines.play.services)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)

    implementation (libs.androidx.work.runtime.ktx.v271)

}
