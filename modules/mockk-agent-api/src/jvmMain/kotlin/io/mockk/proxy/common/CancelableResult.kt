package io.mockk.proxy.common

import io.mockk.proxy.Cancelable
import io.mockk.proxy.MockKAgentException
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

open class CancelableResult<T : Any>(
    input:T?=null,
    private val cancelBlock: () -> Unit = {}
) : Cancelable<T> {

    private val weakValue: WeakReference<T>? = input?.let { WeakReference(it) }

    val fired = AtomicBoolean()

    override fun get() = weakValue?.get()
            ?: throw MockKAgentException("Value for this result is not assigned")

    override fun cancel() {
        if (!fired.getAndSet(true)) {
            cancelBlock()
        }
    }

    fun <R : Any> withValue(value: R) = CancelableResult(value, cancelBlock)

    fun alsoOnCancel(block: () -> Unit) =
        CancelableResult(weakValue?.get()) {
            cancel()
            block()
        }
}
