package io.mockk.proxy.android.transformation

import android.os.Build
import com.android.dx.stock.ProxyBuilder
import com.android.dx.stock.ProxyBuilder.MethodSetEntry
import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.common.ProxyInvocationHandler
import io.mockk.proxy.common.transformation.SubclassInstrumentation
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal class AndroidSubclassInstrumentation(
    val inlineInstrumentationApplied: Boolean
) : SubclassInstrumentation {

    @Suppress("UNCHECKED_CAST")
    override fun <T> subclass(clazz: Class<T>, interfaces: Array<Class<*>>): Class<T> =
        try {
            ProxyBuilder.forClass(clazz)
                .implementing(*interfaces)
                .apply {
                    if (inlineInstrumentationApplied) {
                        onlyMethods(getMethodsToProxy(clazz, interfaces))
                    }
                }
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                        markTrusted();
                    }
                }
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        withSharedClassLoader()
                    }
                }
                .buildProxyClass() as Class<T>
        } catch (e: Exception) {
            throw MockKAgentException("Failed to mock $clazz", e)
        }


    private fun <T> getMethodsToProxy(clazz: Class<T>, interfaces: Array<Class<*>>): Array<Method> {
        val abstractMethods = mutableSetOf<MethodSetEntry>()
        val nonAbstractMethods = mutableSetOf<MethodSetEntry>()

        tailrec fun fillInAbstractAndNonAbstract(clazz: Class<*>) {
            clazz.declaredMethods
                .filter { Modifier.isAbstract(it.modifiers) }
                .map { MethodSetEntry(it) }
                .filterNotTo(abstractMethods) { it in nonAbstractMethods }

            clazz.declaredMethods
                .filterNot { Modifier.isAbstract(it.modifiers) }
                .mapTo(nonAbstractMethods) { MethodSetEntry(it) }

            fillInAbstractAndNonAbstract(clazz.superclass ?: return)
        }

        fillInAbstractAndNonAbstract(clazz)

        fun Class<*>.allSuperInterfaces(): Set<Class<*>> {
            val setOfInterfaces = this.interfaces.toSet()
            return setOfInterfaces + setOfInterfaces.flatMap { it.allSuperInterfaces() }
        }

        (clazz.interfaces + interfaces)
            .asSequence()
            .flatMap { it.allSuperInterfaces().asSequence() }
            .flatMap { it.methods.asSequence() }
            .map { MethodSetEntry(it) }
            .filterNot { it in nonAbstractMethods }
            .mapTo(abstractMethods) { it }

        return abstractMethods.map { it.originalMethod }.toTypedArray()
    }

    override fun setProxyHandler(proxy: Any, handler: MockKInvocationHandler) {
        if (ProxyBuilder.isProxyClass(proxy::class.java)) {
            ProxyBuilder.setInvocationHandler(proxy, ProxyInvocationHandler(handler))
        }
    }

}
