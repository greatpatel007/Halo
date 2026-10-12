plugins {
    alias(libs.plugins.halo.android.library)
    alias(libs.plugins.halo.android.compose)
    alias(libs.plugins.halo.api)
    `maven-publish`
}

android {
    namespace = "com.halo.core.designsystem"
    // Library resources are merged into the host app: the prefix keeps them from colliding with its own.
    resourcePrefix = "halo_"
    // One published variant, with sources so host apps can read the components in the IDE.
    publishing {
        singleVariant("release") { withSourcesJar() }
    }
}

// Public-API guard (`apiDump` / `apiCheck`): dormant until the first release, see HALO_API_CHECK in gradle.properties.

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
    // Predictive back for overlays (sheets, dialogs).
    implementation(libs.androidx.activity.compose)
    // Lifecycle-aware animation: continuous effects pause when the app is in the background.
    implementation(libs.androidx.lifecycle.runtime.compose)
}

// `com.halo:designsystem:<HALO_VERSION>`. `publishReleasePublicationToHaloRepository` writes a Maven repository to
// build/repo (zip it, host it, or point a host app at it); `publishToMavenLocal` installs it in ~/.m2.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = providers.gradleProperty("HALO_GROUP").get()
            artifactId = "designsystem"
            version = providers.gradleProperty("HALO_VERSION").get()
            afterEvaluate { from(components["release"]) }
            pom {
                name.set("Halo design system")
                description.set(
                    "Native Jetpack Compose design system: components, physics-based motion, scene lighting, " +
                        "haptics and sound, and the Tide theme transition. One call: HaloApp { }.",
                )
                url.set("https://github.com/greatpatel007/Halo")
            }
        }
    }
    repositories {
        maven {
            name = "halo"
            url = uri(rootProject.layout.buildDirectory.dir("repo"))
        }
    }
}
