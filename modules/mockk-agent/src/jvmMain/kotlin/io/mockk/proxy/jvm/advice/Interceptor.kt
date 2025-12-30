package io.mockk.proxy.jvm.advice

import io.mockk.core.ValueClassSupport.maybeUnboxValueForMethodReturn
import io.mockk.proxy.MockKInvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.Callable

/**
 * JVM advice that routes intercepted calls to [MockKInvocationHandler] with value-class support.
 */
internal class Interceptor(
    private val handler: MockKInvocationHandler,
    private val self: Any,
    private val method: Method,
    private val arguments: Array<Any?>,
) : Callable<Any?> {
    override fun call(): Any? {
        // Wrap call to eliminate self-recursion when invoking the original implementation.
        val callOriginalMethod =
            SelfCallEliminatorCallable(
                MethodCall(self, method, arguments),
                self,
                method,
            )
        // Delegate to handler and unbox value-class results if needed for the return type.
        return handler.invocation(self, method, callOriginalMethod, arguments)
            ?.maybeUnboxValueForMethodReturn(method)
    }
}
