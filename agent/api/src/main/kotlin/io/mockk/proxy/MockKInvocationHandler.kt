package io.mockk.proxy

import java.lang.reflect.Method
import java.util.concurrent.Callable

interface MockKInvocationHandler {
    @Throws(Exception::class)
    fun invocation(
        self: Any,
        method: Method?,
        originalCall: Callable<*>?,
        args: Array<Any?>
    ): Any?
}
