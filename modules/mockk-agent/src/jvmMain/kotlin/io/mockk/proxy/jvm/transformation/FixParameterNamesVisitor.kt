/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 *
 * Converted to Kotlin from https://github.com/mockito/mockito/blob/76ac001f56e5e718fb668ed8a4f1a5a00d2dae9c/src/main/java/org/mockito/internal/creation/bytebuddy/InlineBytecodeGenerator.java
 */
package io.mockk.proxy.jvm.transformation

import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.field.FieldList
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.method.MethodDescription.CONSTRUCTOR_INTERNAL_NAME
import net.bytebuddy.description.method.MethodList
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.jar.asm.ClassVisitor
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.matcher.ElementMatchers.*
import net.bytebuddy.pool.TypePool
import net.bytebuddy.utility.OpenedClassReader

internal class FixParameterNamesVisitor(val type: Class<*>) :
    AsmVisitorWrapper.AbstractBase() {

    override fun wrap(
        type: TypeDescription,
        visitor: ClassVisitor,
        context: Implementation.Context,
        pool: TypePool,
        fields: FieldList<FieldDescription.InDefinedShape>,
        methods: MethodList<*>,
        writerFlags: Int,
        readerFlags: Int
    ): ClassVisitor {
        return FixParameterNamesClassVisitor(
            visitor,
            TypeDescription.ForLoadedType(this.type)
        )
    }

    internal class FixParameterNamesClassVisitor constructor(
        visitor: ClassVisitor,
        val typeDescription: TypeDescription
    ) : ClassVisitor(OpenedClassReader.ASM_API, visitor) {

        override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor {

            val methodVisitor = super.visitMethod(
                access,
                name,
                desc,
                signature,
                exceptions
            )

            val filter = (when (name) {
                CONSTRUCTOR_INTERNAL_NAME -> isConstructor()
                else -> named<MethodDescription>(name)
            }).and(hasDescriptor(desc))

            val methodList = typeDescription.declaredMethods.filter(filter)

            if (
                methodList.size != 1 ||
                !methodList.only.parameters.hasExplicitMetaData()
            ) {
                return methodVisitor
            }

            for (parameterDescription in methodList.only.parameters) {
                methodVisitor.visitParameter(
                    parameterDescription.name,
                    parameterDescription.modifiers
                )
            }

            return object : MethodVisitor(OpenedClassReader.ASM_API, methodVisitor) {
                override fun visitParameter(name: String, access: Int) {}
            }

        }
    }

}
