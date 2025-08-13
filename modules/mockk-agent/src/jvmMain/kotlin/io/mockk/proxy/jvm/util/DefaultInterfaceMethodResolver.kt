package io.mockk.proxy.jvm.util

import io.mockk.proxy.jvm.advice.MethodCall
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class DefaultInterfaceMethodResolver {

    companion object {

        internal fun getDefaultImplementationOrNull(mock: Any, method: Method, arguments: Array<Any?>): MethodCall? =
            findDefaultImplMethod(method)
                ?.let {
                    val defaultImplMethodArguments = arrayOf(mock, *arguments)
                    MethodCall(mock, it, defaultImplMethodArguments)
                }

        private fun findDefaultImplMethod(method: Method): Method? =
            method.takeIf { Modifier.isAbstract(it.modifiers) }
                ?.declaringClass
                ?.let { declaringClass ->
                    findDefaultImplsClass(declaringClass)
                        ?.runCatching {
                            getMethod(method.name, declaringClass, *method.parameterTypes.requireNoNulls())
                        }
                        ?.getOrNull()
                        ?.takeIf { Modifier.isStatic(it.modifiers) }
                }

        private fun findDefaultImplsClass(clazz: Class<*>): Class<*>? =
            clazz.takeIf { it.isInterface && isKotlinClass(it) }
                ?.classes?.firstOrNull { it.simpleName == "DefaultImpls" && Modifier.isStatic(it.modifiers) }

        private fun isKotlinClass(clazz: Class<*>): Boolean {
            return clazz.isAnnotationPresent(Metadata::class.java)
        }
    }

}
