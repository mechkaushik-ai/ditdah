import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The signing key is supplied by GitHub (from repository secrets) or by your own
// computer through environment variables. It is never stored in the code.
val keystorePath: String? = System.getenv("DITDAH_KEYSTORE")

android {
    namespace = "com.kaushikc.ditdah"
    compileSdk = 36

    defaultConfig {
        // The application ID is the app's permanent identity on Google Play.
        // It can never change after the first public release.
        applicationId = "com.kaushikc.ditdah"
        minSdk = 26
        targetSdk = 36
        // Increase versionCode by 1 for every new release you upload.
        versionCode = 2
        versionName = "1.1.0"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("DITDAH_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DITDAH_KEY_ALIAS")
                keyPassword = System.getenv("DITDAH_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Keeps the APK free of Google's encrypted dependency report, which
    // open-source app stores such as IzzyOnDroid ask developers to remove.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
