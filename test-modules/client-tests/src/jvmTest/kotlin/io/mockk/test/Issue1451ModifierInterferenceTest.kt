package io.mockk.test

import io.mockk.MockKException
import io.mockk.mockk
import io.mockk.mockkClass
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.jar.asm.ClassReader
import net.bytebuddy.jar.asm.ClassVisitor
import net.bytebuddy.jar.asm.ClassWriter
import net.bytebuddy.jar.asm.Opcodes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import kotlin.reflect.KClass
import kotlin.test.assertTrue

/**
 * GitHub issue #1451: `class redefinition failed: attempted to change the class modifiers`.
 *
 * The JVM only accepts a redefinition that changes method bodies. When another tool that
 * instruments classes (a debugger or code-coverage agent) alters a class structure - for example
 * strips a `final` modifier - after it was loaded, or re-emits its own bytes during the
 * retransform chain, the JVM rejects mockk's retransformation of that class.
 *
 * This test reproduces the rejection: the mocked class is defined from bytes whose `ACC_FINAL` was
 * removed the way a load-time instrumenting tool would do it, and a retransform-capable transformer
 * (as the coverage agents register themselves) re-emits the original class file during mockk's
 * retransform. Mocking the class must then fail with the underlying rejection instead of silently
 * producing a mock that never intercepts anything.
 */
class Issue1451ModifierInterferenceTest {
    @Test
    fun `redefinition rejected because class modifiers were altered is reported with its cause`() {
        // Make sure mockk's own transformer is already registered, so the interfering transformer
        // is applied after it and its bytes win the transformer chain.
        mockk<PlainControl>()

        val original = originalBytes(TARGET)
        val stripped = stripFinal(original)

        // Define the class the way a load-time instrumenting tool would leave it: final stripped.
        val altered =
            object : ClassLoader(Issue1451ModifierInterferenceTest::class.java.classLoader) {
                fun define(): Class<*> = defineClass(TARGET, stripped, 0, stripped.size)
            }.define()
        check(
            !java.lang.reflect.Modifier
                .isFinal(altered.modifiers),
        ) {
            "test setup broken: ACC_FINAL was not stripped"
        }

        val instrumentation = ByteBuddyAgent.install()
        val interferer = ReemitOriginalAtRetransform(TARGET, original)
        instrumentation.addTransformer(interferer, true)
        try {
            @Suppress("UNCHECKED_CAST")
            val alteredKlass = altered.kotlin as KClass<Any>
            val failure =
                assertThrows<MockKException> {
                    mockkClass(alteredKlass)
                }

            val chain = generateSequence<Throwable>(failure) { it.cause }.toList()
            assertTrue(
                chain.any { it.message?.contains("redefinition failed") == true },
                "expected the redefinition rejection to be reported, got: $failure",
            )
            assertTrue(
                chain.any {
                    it.message?.contains("AlteredFinal") == true && it.message?.contains("instruments classes") == true
                },
                "expected the diagnosis to name the class and the likely cause, got: $failure",
            )
        } finally {
            instrumentation.removeTransformer(interferer)
        }
    }

    private fun originalBytes(className: String): ByteArray =
        Issue1451ModifierInterferenceTest::class.java.classLoader
            .getResourceAsStream(className.replace('.', '/') + ".class")!!
            .readBytes()

    private fun stripFinal(bytes: ByteArray): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(reader, 0)
        val targetInternalName = TARGET.replace('.', '/')
        reader.accept(
            object : ClassVisitor(Opcodes.ASM9, writer) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String?,
                    signature: String?,
                    superName: String?,
                    interfaces: Array<out String>?,
                ): Unit =
                    writer
                        .visit(
                            version,
                            access and Opcodes.ACC_FINAL.inv(),
                            name,
                            signature,
                            superName,
                            interfaces,
                        ).let { }

                // The class flags and the InnerClasses entry must stay consistent, otherwise the
                // JVM refuses the mismatching class file.
                override fun visitInnerClass(
                    name: String?,
                    outerName: String?,
                    innerName: String?,
                    access: Int,
                ): Unit =
                    writer
                        .visitInnerClass(
                            name,
                            outerName,
                            innerName,
                            if (name == targetInternalName) access and Opcodes.ACC_FINAL.inv() else access,
                        ).let { }
            },
            0,
        )
        return writer.toByteArray()
    }

    private class ReemitOriginalAtRetransform(
        private val target: String,
        private val original: ByteArray,
    ) : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader?,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            classfileBuffer: ByteArray,
        ): ByteArray? =
            when {
                classBeingRedefined != null && className.replace('/', '.') == target -> original
                else -> null
            }
    }

    companion object {
        const val TARGET = "io.mockk.test.Issue1451ModifierInterferenceAlteredFinal"
    }
}

class PlainControl {
    fun value(): Int = 1
}
