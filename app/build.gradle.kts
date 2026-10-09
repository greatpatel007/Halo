plugins {
    alias(libs.plugins.halo.android.application)
    alias(libs.plugins.halo.android.compose)
    alias(libs.plugins.halo.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.halo.wallet"

    defaultConfig {
        applicationId = "com.halo.wallet"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.feature.showroom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
}
