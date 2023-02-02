package io.mockk.proxy.jvm.advice

import io.mockk.core.ValueClassSupport.boxedValue
import io.mockk.proxy.MockKInvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.Callable

internal class Interceptor(
    private val handler: MockKInvocationHandler,
    private val self: Any,
    private val method: Method,
    private val arguments: Array<Any?>
) : Callable<Any?> {

    override fun call(): Any? {
        val callOriginalMethod = SelfCallEliminatorCallable(
            MethodCall(self, method, arguments),
            self,
            method
        )

        return when (val result = handler.invocation(self, method, callOriginalMethod, arguments)) {
            is Result<*> -> result
            else -> result?.boxedValue
        }
    }

}
