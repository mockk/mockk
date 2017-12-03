package io.mockk.impl

import io.mockk.MockKException
import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.MockKGateway.*
import io.mockk.slot


internal class StubberImpl(callRecorderGetter: () -> CallRecorder) : CommonRecorder(callRecorderGetter), Stubber {
    override fun <T> every(mockBlock: (MockKMatcherScope.() -> T)?,
                           coMockBlock: (suspend MockKMatcherScope.() -> T)?): MockKStubScope<T> {
        callRecorder.startStubbing()
        val lambda = slot<Function<*>>()
        val scope = MockKMatcherScope(callRecorder, lambda)
        try {
            record(scope, mockBlock, coMockBlock)
        } catch (ex: Throwable) {
            callRecorder.cancel()
            throw prettifyCoroutinesException(ex)
        }
        checkMissingCalls()
        return MockKStubScope(callRecorder, lambda)
    }

    fun checkMissingCalls() {
        if (callRecorder.calls.isEmpty()) {
            throw MockKException("Missing calls inside every { ... } block.")
        }
    }

}
