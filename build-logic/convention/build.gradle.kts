plugins {
    `kotlin-dsl`
}

group = "com.halo.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.asm.tree)
    implementation(libs.kotlin.metadata.jvm)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.halo.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.halo.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.halo.android.compose.get().pluginId
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.halo.android.feature.get().pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.halo.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("api") {
            id = libs.plugins.halo.api.get().pluginId
            implementationClass = "ApiConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.halo.jvm.library.get().pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
