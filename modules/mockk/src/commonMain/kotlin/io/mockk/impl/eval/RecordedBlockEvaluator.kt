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
            // The failed session is abandoned; a suspended parent session is restored so its
            // owner, still running its own recorded block, is not left with a broken recorder.
            callRecorderInstance.cancelRecordingSession()
            throw InternalPlatform.prettifyRecordingException(ex)
        }
    }

    private class NothingThrownNullPointerException : RuntimeException()

    /**
     * Wraps the recorded block so that a block terminated by a `Nothing`-typed expression is
     * treated as finished, not failed.
     *
     * Two shapes end a recorded block this way:
     *
     * - The last recorded call is declared to return [kotlin.Nothing]. The proxy answers the
     *   recording with `null`, so whatever the block chains on that result throws; the
     *   [checkLastCallReturnsNothing] callback detects the situation from the recorded call.
     *
     * - The block calls a function whose `Nothing` return type comes from a type argument, for
     *   example `mockk<() -> Nothing>()` (#960). The compiler cannot see `Nothing` in the
     *   function's own signature (`Function0.invoke` returns the type variable `R`), but it
     *   knows the expression's type is `Nothing`, so it ends the lambda with an unreachable
     *   `throw KotlinNothingValueException()`. That exception can only originate from such
     *   compiler-inserted code, so it always means "the block ended here" and is safe to
     *   convert to the sentinel without consulting the recorded calls
     *   ([isNothingExpressionTermination]).
     */
    private fun <T> enhanceWithRethrow(
        block: () -> T,
        checkLastCallReturnsNothing: () -> Boolean,
    ): () -> T =
        {
            try {
                block()
            } catch (e: Exception) {
                if (isNothingExpressionTermination(e) || checkLastCallReturnsNothing()) {
                    throw NothingThrownNullPointerException()
                } else {
                    throw e
                }
            }
        }

    protected fun initializeCoroutines() = InternalPlatformDsl.runCoroutine {}
}
