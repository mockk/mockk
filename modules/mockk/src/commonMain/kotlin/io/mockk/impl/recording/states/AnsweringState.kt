package io.mockk.impl.recording.states

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.Invocation
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder

open class AnsweringState(
    recorder: CommonCallRecorder,
) : CallRecordingState(recorder) {
    open val log = recorder.safeToString(Logger<AnsweringState>())

    override fun call(invocation: Invocation): Any? {
        val stub = recorder.stubRepo.stubFor(invocation.self)
        stub.recordCall(invocation.copy(originalCall = { null }))
        try {
            val answer = stub.answer(invocation)
            log.debug { "Answering ${answer.toStr()} on $invocation" }
            return answer
        } catch (ex: Exception) {
            log.debug { "Throwing ${ex.toStr()} on $invocation" }
            throw ex
        }
    }

    override fun startStubbing() = recorder.factories.stubbingState(recorder)

    override fun startVerification(params: VerificationParameters) = recorder.factories.verifyingState(recorder, params)

    /**
     * No recording round is in progress while the recorder idles (or while a session is
     * paused for the coroutine scaffolding that runs a co-block, issue #1247), so there is
     * no last recorded call to inspect. A block failing in this state must propagate its
     * own exception instead of the "bad recording sequence" error the base state raises.
     */
    override fun isLastCallReturnsNothing(): Boolean = false
}
