package io.mockk.impl

import io.mockk.InternalPlatform
import io.mockk.InternalPlatformDsl
import io.mockk.MockKException
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKMatcherScope

open class CommonRecorder(val callRecorder: () -> CallRecorder,
                          val autoHinterFactory: () -> AutoHinter) {

    fun <T, S : MockKMatcherScope> record(scope: S,
                                                   mockBlock: (S.() -> T)?,
                                                   coMockBlock: (suspend S.() -> T)?) {
        try {
            val block: () -> T = if (mockBlock != null) {
                { scope.mockBlock() }
            } else if (coMockBlock != null) {
                { InternalPlatformDsl.runCoroutine { scope.coMockBlock() } }
            } else {
                { throw MockKException("You should specify either 'mockBlock' or 'coMockBlock'") }
            }

            val autoHinter = autoHinterFactory()

            autoHinter.autoHint(
                    callRecorder(),
                    0,
                    64,
                    block)

            val n = callRecorder().estimateCallRounds();
            for (i in 1 until n) {
                autoHinter.autoHint(
                        callRecorder(),
                        i,
                        n,
                        block)
            }
            callRecorder().catchArgs(n, n)
            callRecorder().done()
        } catch (ex: Throwable) {
            throw InternalPlatform.prettifyRecordingException(ex)
        }
    }


    companion object {
        val log = Logger<CommonRecorder>()
    }
}

