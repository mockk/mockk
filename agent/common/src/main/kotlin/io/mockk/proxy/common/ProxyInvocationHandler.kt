/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mockk.proxy.common

import io.mockk.proxy.MockKInvocationHandler

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.Callable

class ProxyInvocationHandler(private val handler: MockKInvocationHandler) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?) = when {
        isEqualsMethod(method) ->
            proxy === args?.get(0)

        isHashCodeMethod(method) ->
            System.identityHashCode(proxy)

        else ->
            handler.invocation(
                proxy,
                method,
                CallProxySuper(proxy, method, args ?: arrayOf()),
                args ?: arrayOf()
            )
    }

    private class CallProxySuper(
        private val proxy: Any,
        private val method: Method,
        private val args: Array<Any?>
    ) : Callable<Any> {

        private fun superMethodName(method: Method): String {
            return "super$${method.name}$" +
                    method.returnType.name
                        .replace('.', '_')
                        .replace('[', '_')
                        .replace(';', '_')

        }

        override fun call() =
            try {
                proxy.javaClass
                    .getMethod(superMethodName(method), *method.parameterTypes)
                    .invoke(proxy, *args)
            } catch (e: InvocationTargetException) {
                throw e.cause!!
            }
    }

    companion object {
        private val equalsMethodName = "equals".intern()
        private val hashCodeMethodName = "hashCode".intern()

        private fun isEqualsMethod(method: Method) = when {
            method.name !== equalsMethodName ->
                false
            method.parameterTypes.size != 1 ->
                false
            else ->
                method.parameterTypes[0] == Any::class.java
        }

        private fun isHashCodeMethod(method: Method) = when {
            method.name !== hashCodeMethodName ->
                false
            else ->
                method.parameterTypes.isEmpty()
        }
    }
}
