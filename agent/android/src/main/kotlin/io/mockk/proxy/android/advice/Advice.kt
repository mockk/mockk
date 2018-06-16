/*
 * Copyright (c) 2018 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package io.mockk.proxy.android.advice

import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.android.AndroidMockKMap
import io.mockk.proxy.android.MethodDescriptor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Modifier.isFinal
import java.lang.reflect.Modifier.isPublic
import java.util.concurrent.Callable

internal class Advice(
    private val handlers: AndroidMockKMap,
    private val staticHandlers: AndroidMockKMap,
    private val constructorHandlers: AndroidMockKMap
) {
    private val selfCallInfo = SelfCallInfo()


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
        if (isInternalHashMap(instance)) {
            return null;
        }

        val instanceOrClass =
            if (Modifier.isStatic(origin.modifiers)) {
                val methodDesc = MethodDescriptor(instance as String)
                MethodDescriptor.classForTypeName(methodDesc.className)
            } else {
                instance
            }

        val handler =
            handlers[instanceOrClass]
                    ?: staticHandlers[instanceOrClass]
                    ?: constructorHandlers[instanceOrClass]
                    ?: return null

        val superMethodCall = SuperMethodCall(
            selfCallInfo,
            origin,
            instanceOrClass,
            arguments
        )

        return Callable {
            handler.invocation(
                instanceOrClass,
                origin,
                superMethodCall,
                arguments
            )
        }
    }

    private fun isInternalHashMap(instance: Any) =
        handlers.isInternalHashMap(instance) ||
                staticHandlers.isInternalHashMap(instance) ||
                constructorHandlers.isInternalHashMap(instance)

    @Suppress("unused") // called from dispatcher
    fun handleConstructor(
        instance: Any,
        methodDescriptor: String,
        arguments: Array<Any?>
    ): Callable<*>? {
        val methodDesc = MethodDescriptor(methodDescriptor)
        val cls = MethodDescriptor.classForTypeName(methodDesc.className)

        val handler =
            constructorHandlers[cls]
                    ?: return null

        return Callable {
            handler.invocation(
                instance,
                null,
                null,
                arguments
            )
        }
    }

    @Suppress("unused") // called from dispatcher
    fun isMock(instance: Any): Boolean {
        if (isInternalHashMap(instance)) {
            return false;
        }
        return handlers.containsKey(instance) ||
                staticHandlers.containsKey(instance) ||
                constructorHandlers.containsKey(instance)
    }

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
            return origin.declaringClass !== method.declaringClass
        }

        private fun Class<*>.findMethod(name: String, parameters: Array<Class<*>>) =
            declaredMethods.firstOrNull {
                it.name == name &&
                        it.parameterTypes refEquals parameters
            }

        private infix fun <T> Array<T>.refEquals(other: Array<T>) =
            size == other.size && zip(other).none { (a, b) -> a !== b }

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
