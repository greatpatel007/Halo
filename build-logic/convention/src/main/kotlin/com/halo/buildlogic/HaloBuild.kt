package com.halo.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** Single source of truth for SDK levels and the JVM target across every module. */
object HaloBuild {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    /** 33 = Android 13: guarantees AGSL RuntimeShader, RenderEffect and haptic compositions. */
    const val MIN_SDK = 33
    val JAVA = JavaVersion.VERSION_17
}

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("Missing library '$alias' in libs.versions.toml") }

fun VersionCatalog.pluginId(alias: String): String =
    findPlugin(alias).orElseThrow { IllegalArgumentException("Missing plugin '$alias' in libs.versions.toml") }
        .get().pluginId
