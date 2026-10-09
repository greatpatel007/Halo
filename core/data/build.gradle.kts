plugins {
    alias(libs.plugins.halo.android.library)
    alias(libs.plugins.halo.hilt)
}

android {
    namespace = "com.halo.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.turbine)
}
