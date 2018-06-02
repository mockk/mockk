package io.mockk.proxy.jvm.advice

import java.lang.reflect.Method
import java.util.concurrent.Callable

internal class MethodCall(
    private val self: Any,
    private val method: Method,
    private val args: Array<Any?>
) : Callable<Any?> {

    override fun call(): Any? {
        method.isAccessible = true
        return method.invoke(self, *args)
    }
}
