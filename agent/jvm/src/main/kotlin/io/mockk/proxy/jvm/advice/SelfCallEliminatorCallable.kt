package io.mockk.proxy.jvm.advice

import java.lang.reflect.Method
import java.util.concurrent.Callable

internal class SelfCallEliminatorCallable(
    private val callable: Callable<Any?>,
    private val self: Any,
    private val method: Method
) : Callable<Any?> {

    override fun call() =
        SelfCallEliminator.apply(self, method) {
            callable.call()
        }
}
