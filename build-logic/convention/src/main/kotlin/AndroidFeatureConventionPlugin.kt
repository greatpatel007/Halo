import com.halo.buildlogic.lib
import com.halo.buildlogic.libs
import com.halo.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature module: Compose UI + ViewModels (Hilt) + type-safe navigation. Features depend on the
 * design system and the domain layer only — never on :core:data (the app wires implementations).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("halo-android-library"))
        pluginManager.apply(libs.pluginId("halo-android-compose"))
        pluginManager.apply(libs.pluginId("halo-hilt"))
        pluginManager.apply(libs.pluginId("kotlin-serialization"))
        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:model"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("kotlinx-serialization-json"))
            add("testImplementation", libs.lib("turbine"))
        }
    }
}
