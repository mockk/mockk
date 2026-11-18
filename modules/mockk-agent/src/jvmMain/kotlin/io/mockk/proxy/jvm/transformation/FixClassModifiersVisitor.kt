package io.mockk.proxy.jvm.transformation

import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.field.FieldList
import net.bytebuddy.description.method.MethodList
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.jar.asm.ClassReader
import net.bytebuddy.jar.asm.ClassVisitor
import net.bytebuddy.pool.TypePool
import net.bytebuddy.utility.OpenedClassReader

/**
 * Keeps the access flags of the class file that is being redefined on the redefined output.
 *
 * Byte Buddy re-derives the class modifiers of a redefined type from the loaded type description.
 * For a member class the JVM reports those modifiers from the `InnerClasses` attribute instead of
 * the class file access flags, so the two views diverge whenever a tool changed the class file
 * without updating the attribute, e.g. when unit tests run against a de-finalized android.jar
 * (Gradle mockable jar). Rewriting such a class then carries the attribute view into the output
 * bytes and the retransformation fails with
 * `class redefinition failed: attempted to change the class modifiers` (#1451).
 *
 * Rewriting the access flags with the ones read from the class file being redefined keeps every
 * retransformation a modifier-preserving change of the method bodies.
 */
internal class FixClassModifiersVisitor(
    classfileBuffer: ByteArray,
) : AsmVisitorWrapper.AbstractBase() {
    private val accessFlags = ClassReader(classfileBuffer).access

    override fun wrap(
        type: TypeDescription,
        visitor: ClassVisitor,
        context: Implementation.Context,
        pool: TypePool,
        fields: FieldList<FieldDescription.InDefinedShape>,
        methods: MethodList<*>,
        writerFlags: Int,
        readerFlags: Int,
    ): ClassVisitor =
        object : ClassVisitor(OpenedClassReader.ASM_API, visitor) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<String>?,
            ) {
                super.visit(
                    version,
                    accessFlags,
                    name,
                    signature,
                    superName,
                    interfaces,
                )
            }
        }
}
