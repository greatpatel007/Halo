package com.halo.buildlogic

import kotlin.metadata.KmClass
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.Visibility
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.visibility
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.zip.ZipFile

/**
 * Renders a library's public API as sorted text, one class per block. What counts is what a host app can call from
 * Kotlin: public/protected classes and members, minus Kotlin `internal` (public in bytecode), private and synthetic
 * ones, and compiler-generated holders.
 */
internal object PublicApi {

    fun render(jars: List<File>, dirs: List<File>): String {
        val classes = buildList {
            dirs.forEach { dir -> dir.walkTopDown().filter { it.extension == "class" }.forEach { add(read(it.readBytes())) } }
            jars.forEach { jar ->
                ZipFile(jar).use { zip ->
                    zip.entries().asSequence().filter { it.name.endsWith(".class") }
                        .forEach { add(read(zip.getInputStream(it).readBytes())) }
                }
            }
        }
        val byName = classes.associateBy { it.name }
        return classes
            .filter { isPublicClass(it, byName) }
            .sortedBy { it.name }
            .joinToString("\n\n") { renderClass(it) } + "\n"
    }

    private fun read(bytes: ByteArray) = ClassNode().also { ClassReader(bytes).accept(it, ClassReader.SKIP_CODE) }

    private fun isPublicClass(node: ClassNode, all: Map<String, ClassNode>): Boolean {
        if (node.access and Opcodes.ACC_SYNTHETIC != 0) return false
        if ("ComposableSingletons" in node.name || node.name.endsWith("\$WhenMappings")) return false
        // Nested classes take their real access from the InnerClasses attribute.
        val inner = node.innerClasses.firstOrNull { it.name == node.name }
        val access = inner?.access ?: node.access
        if (access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) == 0) return false
        val outer = inner?.outerName?.let(all::get)
        if (outer != null && !isPublicClass(outer, all)) return false
        val km = kotlin(node) ?: return true
        return when (km) {
            is KotlinClassMetadata.Class -> km.kmClass.visibility.isPublic()
            is KotlinClassMetadata.FileFacade, is KotlinClassMetadata.MultiFileClassPart -> true
            else -> false
        }
    }

    private fun renderClass(node: ClassNode): String {
        val allowed = kotlin(node)?.let(::publicSignatures)
        val members = buildList {
            node.fields
                .filter { it.access.isApi() && (allowed == null || "field ${it.name}" in allowed) }
                .forEach { add("${modifiers(it.access)}field ${it.name}: ${it.desc}") }
            node.methods
                .filter { it.access.isApi() && (allowed == null || "${it.name}${it.desc}" in allowed || it.name in allowed) }
                .forEach { add("${modifiers(it.access)}fun ${it.name}${it.desc}") }
        }.sorted()
        val kind = when {
            node.access and Opcodes.ACC_ANNOTATION != 0 -> "annotation class"
            node.access and Opcodes.ACC_INTERFACE != 0 -> "interface"
            node.access and Opcodes.ACC_ENUM != 0 -> "enum class"
            else -> "class"
        }
        val header = "${modifiers(node.access and Opcodes.ACC_INTERFACE.inv())}$kind ${node.name}" +
            (node.superName?.takeIf { it != "java/lang/Object" && it != "java/lang/Enum" }?.let { " : $it" } ?: "")
        return if (members.isEmpty()) "$header {}" else members.joinToString("\n", "$header {\n", "\n}") { "\t$it" }
    }

    /**
     * The JVM members Kotlin declares public. Composable functions gain parameters after the metadata was written, so
     * a public function also admits its bytecode by name.
     */
    private fun publicSignatures(km: KotlinClassMetadata): Set<String>? {
        val container: KmDeclarationContainer
        val out = HashSet<String>()
        when (km) {
            is KotlinClassMetadata.Class -> {
                val cls: KmClass = km.kmClass
                container = cls
                cls.constructors.filter { it.visibility.isPublic() }.forEach { c -> c.signature?.let { out += it.toString() } }
                out += "<init>".takeIf { cls.constructors.any { it.visibility.isPublic() } } ?: ""
                if (cls.enumEntries.isNotEmpty()) out += listOf("values", "valueOf", "getEntries")
                out += "field INSTANCE"
                cls.companionObject?.let { out += "field $it" }
                cls.enumEntries.forEach { out += "field $it" }
            }
            is KotlinClassMetadata.FileFacade -> container = km.kmPackage
            is KotlinClassMetadata.MultiFileClassPart -> container = km.kmPackage
            else -> return null
        }
        container.functions.filter { it.visibility.isPublic() }.forEach { f ->
            out += f.name
            f.signature?.let { out += it.toString() }
        }
        container.properties.filter { it.visibility.isPublic() }.forEach { p ->
            p.getterSignature?.let { out += it.toString(); out += it.name }
            p.setterSignature?.let { out += it.toString(); out += it.name }
            // const val and @JvmField live as fields.
            out += "field ${p.name}"
        }
        return out
    }

    private fun kotlin(node: ClassNode): KotlinClassMetadata? {
        val ann: AnnotationNode = node.visibleAnnotations?.firstOrNull { it.desc == "Lkotlin/Metadata;" } ?: return null
        val values = ann.values.chunked(2).associate { (k, v) -> k as String to v }
        @Suppress("UNCHECKED_CAST")
        val metadata = Metadata(
            kind = values["k"] as Int?,
            metadataVersion = (values["mv"] as List<Int>?)?.toIntArray(),
            data1 = (values["d1"] as List<String>?)?.toTypedArray(),
            data2 = (values["d2"] as List<String>?)?.toTypedArray(),
            extraString = values["xs"] as String?,
            packageName = values["pn"] as String?,
            extraInt = values["xi"] as Int?,
        )
        return runCatching { KotlinClassMetadata.readLenient(metadata) }.getOrNull()
    }

    private fun Visibility.isPublic() = this == Visibility.PUBLIC || this == Visibility.PROTECTED

    private fun Int.isApi() = this and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) != 0 && this and Opcodes.ACC_SYNTHETIC == 0

    private fun modifiers(access: Int) = buildString {
        append(if (access and Opcodes.ACC_PROTECTED != 0) "protected " else "public ")
        if (access and Opcodes.ACC_STATIC != 0) append("static ")
        if (access and Opcodes.ACC_ABSTRACT != 0) append("abstract ")
        if (access and Opcodes.ACC_FINAL != 0) append("final ")
    }
}
