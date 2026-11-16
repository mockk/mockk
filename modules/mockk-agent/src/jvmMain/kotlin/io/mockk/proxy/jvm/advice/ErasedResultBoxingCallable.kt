package io.mockk.proxy.jvm.advice

import io.mockk.core.ValueClassSupport.maybeBoxValueForMethodReturn
import java.lang.reflect.Method
import java.util.concurrent.Callable

/**
 * Re-boxes the JVM-erased value-class result of the original method invocation into the
 * instance its Kotlin signature promises, so answer-scope `callOriginal()` can deliver the
 * declared return type (https://github.com/mockk/mockk/issues/1643).
 *
 * Every other result passes through unchanged.
 */
internal class ErasedResultBoxingCallable(
    private val delegate: Callable<Any?>,
    private val method: Method,
) : Callable<Any?> {
    override fun call(): Any? = maybeBoxValueForMethodReturn(method, delegate.call())
}
