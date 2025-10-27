package io.mockk.proxy.jvm.advice

import io.mockk.core.ValueClassSupport.maybeUnboxValueForMethodReturn
import io.mockk.proxy.MockKInvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.Callable

/**
 * JVM advice that routes intercepted calls to the MockK invocation handler and then
 * post-processes the returned value with value-class handling.
 *
 * The post-processing step (`maybeUnboxValueForMethodReturn`) ensures that, when Kotlin value
 * classes are returned through the proxy, MockK returns either the underlying value or the
 * instance itself according to the declared Kotlin return type. This also covers generic
 * return types without invoking value-class getters.
 */
internal class Interceptor(
    private val handler: MockKInvocationHandler,
    private val self: Any,
    private val method: Method,
    private val arguments: Array<Any?>,
) : Callable<Any?> {
    override fun call(): Any? {
        // Eliminate self-calls and obtain a callable to the original method implementation.
        val callOriginalMethod = SelfCallEliminatorCallable(
            MethodCall(self, method, arguments),
            self,
            method
        )
        // Delegate to handler and then normalize value-class results for the method's return type.
        return handler.invocation(self, method, callOriginalMethod, arguments)
                ?.maybeUnboxValueForMethodReturn(method)
    }
}
