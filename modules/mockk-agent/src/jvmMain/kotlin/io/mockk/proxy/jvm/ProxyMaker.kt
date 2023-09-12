package io.mockk.proxy.jvm

import io.mockk.proxy.*
import io.mockk.proxy.common.CancelableResult
import io.mockk.proxy.common.transformation.InlineInstrumentation
import io.mockk.proxy.common.transformation.TransformationRequest
import io.mockk.proxy.common.transformation.TransformationType.SIMPLE
import io.mockk.proxy.jvm.transformation.SubclassInstrumentation
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal class ProxyMaker(
    private val log: MockKAgentLogger,
    private val inliner: InlineInstrumentation?,
    private val subclasser: SubclassInstrumentation,
    private val instantiator: MockKInstantiatior,
    private val handlers: MutableMap<Any, MockKInvocationHandler>
) : MockKProxyMaker {

    override fun <T : Any> proxy(
        clazz: Class<T>,
        interfaces: Array<Class<*>>,
        handler: MockKInvocationHandler,
        useDefaultConstructor: Boolean,
        instance: Any?
    ): Cancelable<T> {

        throwIfNotPossibleToProxy(clazz, interfaces)

        // Sometimes (e.g. in case of sealed classes) we will create the proxy for a subclass of `clazz` and not `clazz`
        // itself.  We need to determine this early, so that the subclass will be inlined as well.
        val actualClass = findActualClassToBeProxied(clazz)

        val cancellation = inline(actualClass)

        val result = CancelableResult<T>(cancelBlock = cancellation)

        val proxyClass = try {
            subclass(actualClass, interfaces)
        } catch (ex: Exception) {
            result.cancel()
            throw MockKAgentException("Failed to subclass $actualClass", ex)
        }

        try {
            val proxy = instantiate(actualClass, proxyClass, useDefaultConstructor, instance)

            handlers[proxy] = handler
            return result
                .withValue(proxy)
                .alsoOnCancel {
                    handlers.remove(proxy)
                }
        } catch (e: Exception) {
            result.cancel()
            throw MockKAgentException("Instantiation exception", e)
        }
    }

    private fun <T : Any> instantiate(
        clazz: Class<T>,
        proxyClass: Class<T>,
        useDefaultConstructor: Boolean,
        instance: Any?
    ): T {
        return when {
            instance != null -> {
                log.trace("Attaching to object mock for $clazz")
                clazz.cast(instance)
            }
            useDefaultConstructor -> {
                log.trace("Instantiating proxy for $clazz via default constructor")
                clazz.cast(newInstanceViaDefaultConstructor(proxyClass))
            }
            else -> {
                log.trace("Instantiating proxy for $clazz via instantiator")
                instantiator.instance(proxyClass)
            }
        }
    }

    private fun <T : Any> inline(
        clazz: Class<T>
    ): () -> Unit {
        val superclasses = getAllSuperclasses(clazz)

        return if (inliner != null) {
            val transformRequest = TransformationRequest(superclasses, SIMPLE)

            inliner.execute(transformRequest)
        } else {
            if (!Modifier.isFinal(clazz.modifiers)) {
                warnOnFinalMethods(clazz)
            }

            { /* No-op */ }
        }
    }

    private fun <T : Any> findActualClassToBeProxied(
        clazz: Class<T>,
    ): Class<T> {
        val kClass = clazz.kotlin
        if (!kClass.isSealed) {
            return clazz
        }

        val subclass = kClass.sealedSubclasses.firstOrNull()?.java
            ?: error("Unable to create proxy for sealed class $clazz, no subclasses available")
        log.trace("Class $clazz is sealed, will use its subclass $subclass to build proxy")
        @Suppress("UNCHECKED_CAST")
        return findActualClassToBeProxied(subclass) as Class<T>
    }

    private fun <T : Any> subclass(
        clazz: Class<T>,
        interfaces: Array<Class<*>>
    ): Class<T> {
        return if (Modifier.isFinal(clazz.modifiers)) {
            log.trace("Taking instance of $clazz itself because it is final.")
            clazz
        } else if (interfaces.isEmpty() && !Modifier.isAbstract(clazz.modifiers) && inliner != null) {
            log.trace("Taking instance of $clazz itself because it is not abstract and no additional interfaces specified.")
            clazz
        } else {
            log.trace(
                "Building subclass proxy for $clazz with " +
                        "additional interfaces ${interfaces.toList()}"
            )
            subclasser.subclass(clazz, interfaces)
        }
    }

    private fun <T : Any> throwIfNotPossibleToProxy(
        clazz: Class<T>,
        interfaces: Array<Class<*>>
    ) {
        when {
            clazz.isPrimitive ->
                throw MockKAgentException(
                    "Failed to create proxy for $clazz.\n$clazz is a primitive"
                )
            clazz.isArray ->
                throw MockKAgentException(
                    "Failed to create proxy for $clazz.\n$clazz is an array"
                )
            clazz as Class<*> in notMockableClasses ->
                throw MockKAgentException(
                    "Failed to create proxy for $clazz.\n$clazz is one of excluded classes"
                )
            interfaces.isNotEmpty() && Modifier.isFinal(clazz.modifiers) ->
                throw MockKAgentException(
                    "Failed to create proxy for $clazz.\nMore interfaces requested and class is final."
                )
        }
    }

    private fun newInstanceViaDefaultConstructor(cls: Class<*>): Any {
        try {
            val defaultConstructor = cls.getDeclaredConstructor()
            try {
                defaultConstructor.isAccessible = true
            } catch (ignored: Exception) {
                // skip
            }

            return defaultConstructor.newInstance()
        } catch (e: Exception) {
            throw MockKAgentException("Default constructor instantiation exception", e)
        }
    }


    private fun warnOnFinalMethods(clazz: Class<*>) {
        for (method in gatherAllMethods(clazz)) {
            val modifiers = method.modifiers
            if (!Modifier.isPrivate(modifiers) && Modifier.isFinal(modifiers)) {
                log.debug(
                    "It is impossible to intercept calls to $method " +
                            "for ${method.declaringClass} because it is final"
                )
            }
        }
    }


    companion object {

        private val notMockableClasses = setOf(
            Class::class.java,
            Boolean::class.java,
            Byte::class.java,
            Short::class.java,
            Char::class.java,
            Int::class.java,
            Long::class.java,
            Float::class.java,
            Double::class.java,
            String::class.java
        )

        private fun gatherAllMethods(clazz: Class<*>): Array<Method> =
            if (clazz.superclass == null) {
                clazz.declaredMethods
            } else {
                gatherAllMethods(clazz.superclass) + clazz.declaredMethods
            }

        private fun getAllSuperclasses(cls: Class<*>): Set<Class<*>> {
            val result = mutableSetOf<Class<*>>()

            var clazz = cls
            while (true) {
                result.add(clazz)
                addInterfaces(result, clazz)
                clazz = clazz.superclass ?: break
            }

            return result
        }

        private fun addInterfaces(result: MutableSet<Class<*>>, clazz: Class<*>) {
            for (intf in clazz.interfaces) {
                if (result.add(intf)) {
                    addInterfaces(result, intf)
                }
            }
        }
    }
}
