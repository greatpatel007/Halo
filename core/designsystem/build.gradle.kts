plugins {
    alias(libs.plugins.halo.android.library)
    alias(libs.plugins.halo.android.compose)
}

android {
    namespace = "com.halo.core.designsystem"
}

// Pure UI: no dependency on model/domain. Only androidx Compose foundations — no Material, no
// third-party UI or animation libraries. Every component is hand-built.
dependencies {
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.ui.text)
    api(libs.compose.foundation)
    api(libs.compose.animation)
    api(libs.androidx.graphics.shapes)
    implementation(libs.androidx.core.ktx)
    // Lifecycle-aware animation: continuous effects pause when the app is in the background.
    implementation(libs.androidx.lifecycle.runtime.compose)
}
