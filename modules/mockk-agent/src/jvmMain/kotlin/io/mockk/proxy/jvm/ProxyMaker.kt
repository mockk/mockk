package io.mockk.proxy.jvm

import io.mockk.proxy.Cancelable
import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.MockKInstantiatior
import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.MockKProxyMaker
import io.mockk.proxy.common.CancelableResult
import io.mockk.proxy.common.transformation.InlineInstrumentation
import io.mockk.proxy.common.transformation.TransformationRequest
import io.mockk.proxy.common.transformation.TransformationType.SIMPLE
import io.mockk.proxy.jvm.transformation.SubclassInstrumentation
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError

internal class ProxyMaker(
    private val log: MockKAgentLogger,
    private val inliner: InlineInstrumentation?,
    private val subclasser: SubclassInstrumentation,
    private val instantiator: MockKInstantiatior,
    private val handlers: MutableMap<Any, MockKInvocationHandler>,
) : MockKProxyMaker {
    /**
     * `Class.isSealed()` exists since JDK 17; resolved reflectively so this file stays loadable
     * on JDK 8-14.
     */
    private val isSealedMethod: Method? =
        runCatching {
            Class::class.java.getMethod("isSealed")
        }.getOrNull()

    override fun <T : Any> proxy(
        clazz: Class<T>,
        interfaces: Array<Class<*>>,
        handler: MockKInvocationHandler,
        useDefaultConstructor: Boolean,
        instance: Any?,
    ): Cancelable<T> {
        throwIfNotPossibleToProxy(clazz, interfaces)

        // Sometimes (e.g. in case of sealed classes) we will create the proxy for a subclass of `clazz` and not `clazz`
        // itself.  We need to determine this early, so that the subclass will be inlined as well.
        val actualClass = findActualClassToBeProxied(clazz)

        val cancellation = inline(actualClass)

        val result = CancelableResult<T>(cancelBlock = cancellation)

        val proxyClass =
            try {
                subclass(actualClass, interfaces)
            } catch (ex: Exception) {
                result.cancel()
                throw MockKAgentException("Failed to subclass $actualClass", ex)
            }

        try {
            val proxy = instantiate(actualClass, proxyClass, useDefaultConstructor, instance)

            handlers[proxy] = handler
            // The handler map keys the proxy weakly, and the stub behind the handler keeps this cancelable.
            // Holding the proxy strongly here would keep the key reachable from its own value, so it never clears.
            val proxyRef = WeakReference(proxy)
            val cancelable =
                result
                    .withValue(proxy)
                    .alsoOnCancel {
                        proxyRef.get()?.let { handlers.remove(it) }
                    }
            return ProxyCancelable(proxy, cancelable)
        } catch (e: Exception) {
            result.cancel()
            throw MockKAgentException("Instantiation exception", e)
        }
    }

    private fun <T : Any> instantiate(
        clazz: Class<T>,
        proxyClass: Class<T>,
        useDefaultConstructor: Boolean,
        instance: Any?,
    ): T =
        when {
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

    private fun <T : Any> inline(clazz: Class<T>): () -> Unit {
        // A hidden class cannot be retransformed by the agent (JEP 371); requesting its
        // transformation would only produce a "non instrumentable" warning on every mock.
        val superclasses = getAllSuperclasses(clazz).filterNot { HiddenClasses.isHidden(it) }.toSet()

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

    private fun <T : Any> findActualClassToBeProxied(clazz: Class<T>): Class<T> {
        if (!isSealed(clazz)) {
            return clazz
        }

        val subclass =
            clazz.kotlin.sealedSubclasses
                .firstOrNull()
                ?.java
                ?: error("Unable to create proxy for sealed class $clazz, no subclasses available")
        log.trace("Class $clazz is sealed, will use its subclass $subclass to build proxy")
        @Suppress("UNCHECKED_CAST")
        return findActualClassToBeProxied(subclass) as Class<T>
    }

    /**
     * `KClass.isSealed` for classes kotlin-reflect cannot describe: compiler-synthesized lambda
     * classes (Kotlin 1.x named lambda classes and `invokedynamic` lambdas,
     * [#1313](https://github.com/mockk/mockk/issues/1313)) make it throw
     * [UnsupportedOperationException] or [KotlinReflectionInternalError]. Such classes are never
     * sealed, so `Class.isSealed()` (JDK 17+, resolved reflectively to stay loadable on JDK 8-14)
     * answers in their place.
     */
    private fun isSealed(clazz: Class<*>): Boolean =
        try {
            clazz.kotlin.isSealed
        } catch (suppressed: UnsupportedOperationException) {
            isSealedByJvm(clazz)
        } catch (suppressed: KotlinReflectionInternalError) {
            isSealedByJvm(clazz)
        }

    private fun isSealedByJvm(clazz: Class<*>): Boolean =
        runCatching {
            isSealedMethod?.invoke(clazz) == true
        }.getOrDefault(false)

    private fun <T : Any> subclass(
        clazz: Class<T>,
        interfaces: Array<Class<*>>,
    ): Class<T> =
        when {
            HiddenClasses.isHidden(clazz) -> {
                log.trace(
                    "Hidden class $clazz cannot be subclassed nor instrumented; " +
                        "building proxy over its interfaces delegating to the original instance.",
                )
                subclasser.delegateSubclass(clazz, interfaces)
            }
            Modifier.isFinal(clazz.modifiers) -> {
                log.trace("Taking instance of $clazz itself because it is final.")
                clazz
            }
            interfaces.isEmpty() && !Modifier.isAbstract(clazz.modifiers) && inliner != null -> {
                log.trace("Taking instance of $clazz itself because it is not abstract and no additional interfaces specified.")
                clazz
            }
            else -> {
                log.trace(
                    "Building subclass proxy for $clazz with " +
                        "additional interfaces ${interfaces.toList()}",
                )
                subclasser.subclass(clazz, interfaces)
            }
        }

    private fun <T : Any> throwIfNotPossibleToProxy(
        clazz: Class<T>,
        interfaces: Array<Class<*>>,
    ) {
        when {
            clazz.isPrimitive ->
                throw MockKAgentException(
                    "Failed to create proxy for $clazz.\n$clazz is a primitive",
                )
            clazz.isArray ->
                throw MockKAgentException(
                    "Failed to create proxy for $clazz.\n$clazz is an array",
                )
            clazz as Class<*> in notMockableClasses ->
                throw MockKAgentException(
                    "Failed to create proxy for $clazz.\n$clazz is one of excluded classes",
                )
            interfaces.isNotEmpty() &&
                Modifier.isFinal(clazz.modifiers) &&
                !HiddenClasses.isHidden(clazz) ->
                throw MockKAgentException(
                    "Failed to create proxy for $clazz.\nMore interfaces requested and class is final.",
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
                        "for ${method.declaringClass} because it is final",
                )
            }
        }
    }

    /**
     * Keeps the proxy strongly reachable until the caller takes it with the first [get], so a GC in between
     * cannot clear it (#997). After that the caller owns the proxy and this cancelable only refers to it weakly.
     */
    private class ProxyCancelable<T : Any>(
        proxy: T,
        private val delegate: Cancelable<T>,
    ) : Cancelable<T> {
        @Volatile
        private var untaken: T? = proxy

        override fun get(): T = untaken?.also { untaken = null } ?: delegate.get()

        override fun cancel() = delegate.cancel()
    }

    companion object {
        private val notMockableClasses =
            setOf(
                Class::class.java,
                Boolean::class.java,
                Byte::class.java,
                Short::class.java,
                Char::class.java,
                Int::class.java,
                Long::class.java,
                Float::class.java,
                Double::class.java,
                String::class.java,
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

        private fun addInterfaces(
            result: MutableSet<Class<*>>,
            clazz: Class<*>,
        ) {
            for (intf in clazz.interfaces) {
                if (result.add(intf)) {
                    addInterfaces(result, intf)
                }
            }
        }
    }
}
