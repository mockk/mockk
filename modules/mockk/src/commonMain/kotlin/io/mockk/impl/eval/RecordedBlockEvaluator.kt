package io.mockk.impl.eval

import io.mockk.InternalPlatformDsl
import io.mockk.MockKException
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKMatcherScope
import io.mockk.impl.InternalPlatform
import io.mockk.impl.recording.AutoHinter

abstract class RecordedBlockEvaluator(
    val callRecorder: () -> CallRecorder,
    val autoHinterFactory: () -> AutoHinter,
) {
    fun <T, S : MockKMatcherScope> record(
        scope: S,
        mockBlock: (S.() -> T)?,
        coMockBlock: (suspend S.() -> T)?,
    ) {
        val callRecorderInstance = callRecorder()
        return try {
            val block: () -> T =
                when {
                    mockBlock != null -> {
                        { scope.mockBlock() }
                    }
                    coMockBlock != null -> {
                        {
                            // The coroutine that runs the block resolves its own context on
                            // entry, which with a static mock on a global object (for example
                            // Dispatchers) is an intercepted call. Keep those scaffolding
                            // calls out of the recorded block: pause the session until the
                            // coroutine is running the block itself (issue #1247).
                            callRecorderInstance.pauseSession()
                            InternalPlatformDsl.runCoroutine {
                                callRecorderInstance.resumeSession()
                                scope.coMockBlock()
                            }
                        }
                    }
                    else -> {
                        { throw MockKException("You should specify either 'mockBlock' or 'coMockBlock'") }
                    }
                }

            val blockWithRethrow = enhanceWithRethrow(block, callRecorderInstance::isLastCallReturnsNothing)

            val autoHinter = autoHinterFactory()

            try {
                autoHinter.autoHint(
                    callRecorderInstance,
                    0,
                    64,
                    blockWithRethrow,
                )
            } catch (npe: NothingThrownNullPointerException) {
                // skip
            }

            val n = callRecorderInstance.estimateCallRounds()
            for (i in 1 until n) {
                try {
                    autoHinter.autoHint(
                        callRecorderInstance,
                        i,
                        n,
                        blockWithRethrow,
                    )
                } catch (npe: NothingThrownNullPointerException) {
                    // skip
                }
            }
            callRecorderInstance.round(n, n)
            callRecorderInstance.done()
        } catch (ex: Throwable) {
            // A failed session can leave nested suspended sessions without an owner: drop them all.
            callRecorderInstance.cancelRecordingSession()
            throw InternalPlatform.prettifyRecordingException(ex)
        }
    }

    private class NothingThrownNullPointerException : RuntimeException()

    private fun <T> enhanceWithRethrow(
        block: () -> T,
        checkLastCallReturnsNothing: () -> Boolean,
    ): () -> T =
        {
            try {
                block()
            } catch (e: Exception) {
                if (checkLastCallReturnsNothing()) {
                    throw NothingThrownNullPointerException()
                } else {
                    throw e
                }
            }
        }

    protected fun initializeCoroutines() = InternalPlatformDsl.runCoroutine {}
}
