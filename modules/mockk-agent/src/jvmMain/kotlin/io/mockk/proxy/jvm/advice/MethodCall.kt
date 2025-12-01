package io.mockk.proxy.jvm.advice

import java.lang.reflect.Method
import java.util.concurrent.Callable

internal class MethodCall(
    private val self: Any,
    private val method: Method,
    private val args: Array<Any?>
) : Callable<Any?> {

    override fun call(): Any? {
        try {
            method.isAccessible = true
        } catch (ignored: Exception) {
            // Skip setting accessible - method may be in a JDK module that doesn't open to unnamed modules (JDK 16+)
        }
        return method.invoke(self, *args)
    }
}
