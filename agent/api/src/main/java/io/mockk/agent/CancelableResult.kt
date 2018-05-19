package io.mockk.agent

import java.util.concurrent.atomic.AtomicBoolean

open class CancelableResult<T : Any>(
    private val value: T? = null,
    private val cancelBlock: () -> Unit = {}
) : Cancelable<T> {

    val fired = AtomicBoolean()

    override fun get() = value
            ?: throw MockKAgentException("Value for this result is not assigned")

    override fun cancel() {
        if (!fired.getAndSet(true)) {
            cancelBlock()
        }
    }

    fun <R : Any> withValue(value: R) = CancelableResult(value, cancelBlock)

    fun alsoOnCancel(block: () -> Unit) =
        CancelableResult(value) {
            cancelBlock()
            block()
        }
}
