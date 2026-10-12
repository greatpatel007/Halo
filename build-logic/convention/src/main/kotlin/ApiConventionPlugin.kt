import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.halo.buildlogic.PublicApi
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Guards a published library's public API (proposal 0004 step 3). `apiDump` writes `api/<project>.api` from the
 * release classes; `apiCheck` (part of `check`) fails when the API changed without a new dump, so an accidental
 * break for host apps can't slip through. Kotlin `internal`, private and synthetic members are left out.
 *
 * Off during development (the API changes with every component): `apiCheck` joins `check` only with
 * `HALO_API_CHECK=true` in gradle.properties, to be switched on with the first release (proposal 0004).
 *
 * (Kotlin's own ABI validation doesn't see AGP 9's built-in Kotlin compilation yet, hence this small plugin.)
 */
class ApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val dumpFile = layout.projectDirectory.file("api/${name}.api")
        val dump = tasks.register<ApiDumpTask>("apiDump") {
            group = "verification"
            description = "Writes the public API of the release variant to api/${project.name}.api."
            output.set(dumpFile)
        }
        val check = tasks.register<ApiCheckTask>("apiCheck") {
            group = "verification"
            description = "Fails if the public API differs from api/${project.name}.api (run apiDump after an intended change)."
            reference.set(dumpFile)
            actual.set(layout.buildDirectory.file("api/${project.name}.api"))
        }
        val components = extensions.getByType<LibraryAndroidComponentsExtension>()
        components.onVariants(components.selector().withName("release")) { variant ->
            listOf(dump, check).forEach { task ->
                variant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
                    .use(task)
                    .toGet(ScopedArtifact.CLASSES, ApiTask::jars, ApiTask::dirs)
            }
        }
        if (providers.gradleProperty("HALO_API_CHECK").orNull == "true") {
            tasks.named("check") { dependsOn(check) }
        }
    }
}

abstract class ApiTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dirs: ListProperty<Directory>

    protected fun render(): String = PublicApi.render(jars.get().map { it.asFile }, dirs.get().map { it.asFile })
}

abstract class ApiDumpTask : ApiTask() {
    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun dump() {
        output.get().asFile.apply { parentFile.mkdirs() }.writeText(render())
    }
}

abstract class ApiCheckTask : ApiTask() {
    @get:Internal
    abstract val reference: RegularFileProperty

    @get:OutputFile
    abstract val actual: RegularFileProperty

    @TaskAction
    fun check() {
        val now = render()
        actual.get().asFile.apply { parentFile.mkdirs() }.writeText(now)
        val ref = reference.get().asFile
        if (!ref.exists()) throw GradleException("No API dump at $ref. Run apiDump and commit it.")
        val before = ref.readLines().toSet()
        val after = now.lines().toSet()
        val removed = (before - after).filter { it.isNotBlank() }
        val added = (after - before).filter { it.isNotBlank() }
        if (removed.isNotEmpty() || added.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("The public API changed. If intended, run apiDump and commit ${ref.name}.")
                    removed.take(LIMIT).forEach { appendLine("  - $it") }
                    added.take(LIMIT).forEach { appendLine("  + $it") }
                },
            )
        }
    }

    private companion object {
        const val LIMIT = 40
    }
}
