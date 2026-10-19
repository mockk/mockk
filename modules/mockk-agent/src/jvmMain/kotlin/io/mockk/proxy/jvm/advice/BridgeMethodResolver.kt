package io.mockk.proxy.jvm.advice

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Finds the method a compiler-generated bridge method stands for.
 *
 * A bridge has the erased signature of an overridden member and the same name as the overriding
 * method, which is declared in the bridge's class or, for a visibility bridge, in a superclass.
 * Calls through a bridge are recorded against that method, so they match stubs and verifications
 * written against the member itself.
 */
internal object BridgeMethodResolver {
    /**
     * @return the method [bridge] stands for, or `null` when it cannot be determined unambiguously
     * or [arguments] do not fit its parameters. The bridge then keeps running its own body.
     */
    fun findBridgedMethod(
        bridge: Method,
        arguments: Array<Any?>,
    ): Method? =
        findBridgedMethod(bridge)
            ?.takeIf { it.accepts(arguments) }

    private fun findBridgedMethod(bridge: Method): Method? {
        var clazz: Class<*>? = bridge.declaringClass
        while (clazz != null) {
            val candidates = clazz.declaredMethods.filter { it.isBridgedBy(bridge) }
            if (candidates.isNotEmpty()) {
                return candidates.singleOrNull()
            }
            clazz = clazz.superclass
        }
        return null
    }

    private fun Method.isBridgedBy(bridge: Method): Boolean =
        !isBridge &&
            !Modifier.isStatic(modifiers) &&
            name == bridge.name &&
            parameterCount == bridge.parameterCount &&
            bridge.returnType.objectType.isAssignableFrom(returnType.objectType) &&
            parameterTypes.zip(bridge.parameterTypes).all { (type, bridgeType) ->
                bridgeType.objectType.isAssignableFrom(type.objectType)
            }

    private fun Method.accepts(arguments: Array<Any?>): Boolean =
        parameterTypes.size == arguments.size &&
            parameterTypes.zip(arguments).all { (type, argument) ->
                if (argument == null) !type.isPrimitive else type.objectType.isInstance(argument)
            }

    private val Class<*>.objectType: Class<*>
        get() = if (isPrimitive) kotlin.javaObjectType else this
}
