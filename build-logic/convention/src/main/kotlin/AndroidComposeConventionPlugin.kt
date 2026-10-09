import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.halo.buildlogic.lib
import com.halo.buildlogic.libs
import com.halo.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Enables Compose on an Android application or library module and adds the BOM + tooling. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-compose"))
        pluginManager.withPlugin(libs.pluginId("android-application")) {
            extensions.configure<ApplicationExtension> { buildFeatures { compose = true } }
        }
        pluginManager.withPlugin(libs.pluginId("android-library")) {
            extensions.configure<LibraryExtension> { buildFeatures { compose = true } }
        }
        dependencies {
            val bom = platform(libs.lib("compose-bom"))
            add("implementation", bom)
            add("implementation", libs.lib("compose-ui-tooling-preview"))
            add("debugImplementation", libs.lib("compose-ui-tooling"))
        }
    }
}
