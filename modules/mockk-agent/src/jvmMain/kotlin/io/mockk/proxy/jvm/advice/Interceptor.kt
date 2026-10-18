package io.mockk.proxy.jvm.advice

import io.mockk.core.ValueClassSupport.maybeUnboxValueForMethodReturn
import io.mockk.proxy.MockKInvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.Callable

/**
 * JVM advice that routes intercepted calls to [MockKInvocationHandler] with value-class support.
 *
 * [method] is the method the call is recorded against. [originalMethod] is the method that was
 * actually intercepted; it differs from [method] only for a bridge method, which is recorded
 * against the method it bridges but still runs its own body when the original is called.
 */
internal class Interceptor(
    private val handler: MockKInvocationHandler,
    private val self: Any,
    private val method: Method,
    private val arguments: Array<Any?>,
    private val originalMethod: Method = method,
) : Callable<Any?> {
    override fun call(): Any? {
        // Wrap call to eliminate self-recursion when invoking the original implementation.
        val callOriginalMethod =
            SelfCallEliminatorCallable(
                MethodCall(self, originalMethod, arguments),
                self,
                originalMethod,
            )
        // Delegate to handler and unbox value-class results if needed for the return type.
        return handler
            .invocation(self, method, callOriginalMethod, arguments)
            ?.maybeUnboxValueForMethodReturn(originalMethod)
    }
}
