import com.android.build.api.dsl.LibraryExtension
import com.halo.buildlogic.HaloBuild
import com.halo.buildlogic.lib
import com.halo.buildlogic.libs
import com.halo.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-library"))
        extensions.configure<LibraryExtension> {
            compileSdk = HaloBuild.COMPILE_SDK
            defaultConfig {
                minSdk = HaloBuild.MIN_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = HaloBuild.JAVA
                targetCompatibility = HaloBuild.JAVA
            }
        }
        dependencies {
            add("testImplementation", libs.lib("junit"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        }
    }
}
