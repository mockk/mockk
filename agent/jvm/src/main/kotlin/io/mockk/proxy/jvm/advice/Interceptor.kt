package io.mockk.proxy.jvm.advice

import io.mockk.proxy.MockKInvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.Callable

internal class Interceptor(
    private val handler: MockKInvocationHandler,
    private val self: Any,
    private val method: Method,
    private val arguments: Array<Any?>,
    val orignalMethodCall: Callable<*>
) : Callable<Any?> {

    override fun call(): Any? {
        return handler.invocation(self, method, orignalMethodCall, arguments)
    }

}
