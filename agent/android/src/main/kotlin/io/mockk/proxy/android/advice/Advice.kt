/*
 * Copyright (c) 2018 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package io.mockk.proxy.android.advice

import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.android.MethodDescriptor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Modifier.isFinal
import java.lang.reflect.Modifier.isPublic
import java.util.concurrent.Callable

internal class Advice(
    private val handlers: Map<Any, MockKInvocationHandler>
) {
    private val selfCallInfo = SelfCallInfo()

    private val classMocks
        get() = handlers.keys.filterIsInstance<Class<*>>()


    @Suppress("unused") // called from dispatcher
    fun getOrigin(instance: Any?, methodWithTypeAndSignature: String): Method? {
        val methodDesc = MethodDescriptor(methodWithTypeAndSignature)

        val obj = instance
                ?: MethodDescriptor.classForTypeName(methodDesc.className)

        if (!obj.checkSelfCall()) {
            return null
        }

        if (instance != null && instance::class.java.isOverridden(methodDesc.method)) {
            return null
        }

        return methodDesc.method
    }


    @Suppress("unused") // called from dispatcher
    fun handle(
        instance: Any,
        origin: Method,
        arguments: Array<Any?>
    ): Callable<*>? {
        val instanceOrClass =
            if (Modifier.isStatic(origin.modifiers)) {
                val methodDesc = MethodDescriptor(instance as String)
                MethodDescriptor.classForTypeName(methodDesc.className)
            } else {
                instance
            }

        val handler = handlers[instanceOrClass] ?: return null

        val superMethodCall = SuperMethodCall(
            selfCallInfo,
            origin,
            instanceOrClass,
            arguments
        )

        val result = handler.invocation(
            instanceOrClass,
            origin,
            superMethodCall,
            arguments ?: arrayOf()
        )

        return Callable { result }
    }

    @Suppress("unused") // called from dispatcher
    fun isMock(instance: Any) =
        instance !== handlers && handlers.containsKey(instance)

    private fun Any.checkSelfCall() = selfCallInfo.checkSelfCall(this)

    private class SuperMethodCall(
        private val selfCallInfo: SelfCallInfo,
        private val origin: Method,
        private val instance: Any,
        private val arguments: Array<Any?>
    ) : Callable<Any?> {
        override fun call(): Any? = try {
            origin.makeAccessible()
            selfCallInfo.set(instance)
            origin.invoke(instance, *arguments)
        } catch (exception: InvocationTargetException) {
            throw exception.cause
                    ?: MockKAgentException("no cause for InvocationTargetException", exception)
        }

    }

    private class SelfCallInfo : ThreadLocal<Any>() {
        fun checkSelfCall(value: Any) =
            if (get() === value) {
                set(null)
                false
            } else {
                true
            }
    }

    companion object {
        private tailrec fun Class<*>.isOverridden(origin: Method): Boolean {
            val method = findMethod(origin.name, origin.parameterTypes)
                    ?: return superclass.isOverridden(origin)
            return origin.declaringClass != method.declaringClass
        }

        private fun Class<*>.findMethod(name: String, parameters: Array<Class<*>>) =
            declaredMethods.firstOrNull {
                it.name == name &&
                        it.parameterTypes contentEquals parameters
            }

        @Synchronized
        @JvmStatic
        private external fun nativeGetCalledClassName(): String

        private val Class<*>.final
            get() = isFinal(modifiers)

        private val Method.final
            get() = isFinal(modifiers)

        private fun Method.makeAccessible() {
            if (!isPublic(declaringClass.modifiers and this.modifiers)) {
                isAccessible = true
            }
        }
    }
}
