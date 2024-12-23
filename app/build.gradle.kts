plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.meenbeese.chronos"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.meenbeese.chronos"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
    }

    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    // Core
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Media
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Material
    implementation("com.google.android.material:material:1.12.0")

    // FAB
    implementation("com.leinardi.android:speed-dial:3.3.0")

    // Image
    implementation("io.coil-kt:coil:2.7.0")
}
