package io.mockk.jvm

import io.mockk.MethodDescription
import io.mockk.MockKException
import io.mockk.common.Stub
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.Callable

internal object JvmMockFactoryHelper {
    fun mockHandler(stub: Stub): (Any, Method, Callable<*>, Array<Any?>) -> Any? {
        return { self, method, originalMethod, args ->
            stdFunctions(self, method, args) {
                stub.handleInvocation(self, method.toDescription(), {
                    handleOriginalCall(originalMethod, method)
                }, args)
            }
        }
    }

    inline fun stdFunctions(self: Any,
                            method: Method,
                            args: Array<Any?>,
                            otherwise: () -> Any?): Any? {
        if (self is Class<*>) {
            if (method.isHashCode()) {
                return System.identityHashCode(self)
            } else if (method.isEquals()) {
                return self === args[0]
            }
        }
        return otherwise()
    }

    fun handleOriginalCall(originalMethod: Callable<*>?, method: Method): Any? {
        if (originalMethod == null) {
            throw MockKException("No way to call original method ${method.toDescription()}")
        }

        return try {
            originalMethod.call()
        } catch (ex: InvocationTargetException) {
            throw MockKException("Failed to execute original call. Check cause please", ex.cause)
        }
    }


    fun Method.toDescription() =
            MethodDescription(name, returnType.kotlin, declaringClass.kotlin, parameterTypes.map { it.kotlin })

    fun Method.isHashCode() = name == "hashCode" && parameterTypes.isEmpty()
    fun Method.isEquals() = name == "equals" && parameterTypes.size == 1 && parameterTypes[0] === Object::class.java
}