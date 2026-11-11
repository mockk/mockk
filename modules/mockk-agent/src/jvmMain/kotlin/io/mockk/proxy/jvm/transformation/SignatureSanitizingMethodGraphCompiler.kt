package io.mockk.proxy.jvm.transformation

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.method.MethodList
import net.bytebuddy.description.type.TypeDefinition
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.scaffold.MethodGraph

/**
 * Wraps a method graph compiler and stands in erasure-only method descriptions for methods whose
 * generic signatures cannot be resolved.
 *
 * Kotlin compiles a function that takes a `@JvmInline` value class whose underlying type is generic
 * (e.g. Arrow's `NonEmptySet<out E>` backed by a `Set<E>`) into a name-mangled method taking the
 * erasure of that underlying type. The Kotlin compiler fails to substitute the actual type argument
 * into the mangled method's `Signature` attribute, leaving something like
 * `(Ljava/util/Set<+TA;>;)Ljava/lang/String;` behind, where the type variable `A` is not declared
 * by any type visible to that signature. Reflecting over such a method yields a wildcard with no
 * upper bound, and Byte Buddy throws from `MethodDescription.getGenericSignature()` while writing
 * the attributes of the generated proxy method, so the proxy cannot be created at all (#1360,
 * #1410).
 *
 * The stand-in keeps the original name, modifiers and erased shape, but carries no type variables
 * and no generic signature: Byte Buddy then writes the generated method without a `Signature`
 * attribute instead of failing. The proxy methods are interceptor bridges that forward every call
 * to mockk's dispatcher, and recording happens against the reflective `Method` of the original
 * type, so neither stubbing nor verification can tell the difference.
 */
internal class SignatureSanitizingMethodGraphCompiler(
    private val delegate: MethodGraph.Compiler,
) : MethodGraph.Compiler.AbstractBase() {
    override fun compile(
        typeDefinition: TypeDefinition,
        viewPoint: TypeDescription,
    ): MethodGraph.Linked {
        val graph = delegate.compile(typeDefinition, viewPoint)

        return object : MethodGraph.Linked by graph {
            override fun listNodes(): MethodGraph.NodeList =
                MethodGraph.NodeList(
                    graph.listNodes().map(::sanitize),
                )
        }
    }

    private fun sanitize(node: MethodGraph.Node): MethodGraph.Node =
        if (node.sort.isResolved && node.representative.hasUnresolvableGenericSignature()) {
            MethodGraph.Node.Simple(node.representative.asErasureOnlyDescription())
        } else {
            node
        }
}

/**
 * A description of a loaded type whose declared methods with unresolvable generic signatures are
 * replaced by erasure-only stand-ins (see [SignatureSanitizingMethodGraphCompiler] for why such
 * signatures exist and what the stand-ins do).
 *
 * Used to redefine a class: the type writer reads the redefined type's declared methods to write
 * each method's attributes, and reflecting over a mangled method's generic signature fails there
 * just like it does during subclass generation.
 */
internal class SignatureSanitizedTypeDescription(
    type: Class<*>,
) : TypeDescription.ForLoadedType(type) {
    override fun getDeclaredMethods(): MethodList<MethodDescription.InDefinedShape> =
        MethodList.Explicit(
            super
                .getDeclaredMethods()
                .map { method: MethodDescription.InDefinedShape ->
                    if (method.hasUnresolvableGenericSignature()) method.asErasureOnlyDescription() else method
                },
        )
}

internal fun MethodDescription.hasUnresolvableGenericSignature(): Boolean =
    try {
        genericSignature
        false
    } catch (ex: Throwable) {
        true
    }

private fun MethodDescription.asErasureOnlyDescription(): MethodDescription.InDefinedShape =
    MethodDescription.Latent(
        declaringType.asErasure(),
        MethodDescription.Token(
            name,
            modifiers,
            TypeDescription.Generic.OfNonGenericType.ForErasure(returnType.asErasure()),
            parameters.asTypeList().asErasures().map { TypeDescription.Generic.OfNonGenericType.ForErasure(it) },
        ),
    )
