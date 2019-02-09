package io.mockk.impl.recording.states

import io.mockk.*
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.AdditionalAnswerOpportunity
import io.mockk.impl.stub.MockKStub

class StubbingAwaitingAnswerState(recorder: CommonCallRecorder) : CallRecordingState(recorder) {
    val log = recorder.safeToString(Logger<StubbingAwaitingAnswerState>())

    override fun answer(answer: Answer<*>) {
        val calls = recorder.calls

        var answerOpportunity: AdditionalAnswerOpportunity? = null

        for ((idx, recordedCall) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val ans = if (lastCall) {
                answer
            } else if (recordedCall.isRetValueMock) {
                ConstantAnswer(recordedCall.retValue)
            } else {
                continue
            }

            val mock = recordedCall.matcher.self
            val stub = recorder.stubRepo.stubFor(mock)
            answerOpportunity = stub
                .addAnswer(recordedCall.matcher, ans)

            if (stub::class == MockKStub::class) {
                assignFieldIfMockingProperty(mock, recordedCall.matcher, ans)
            }
        }

        calls.clear()

        log.trace { "Done stubbing. Still accepting additional answers" }

        recorder.state = recorder.factories.answeringStillAcceptingAnswersState(recorder, answerOpportunity!!)
    }

    override fun call(invocation: Invocation): Any? {
        val stub = recorder.stubRepo.stubFor(invocation.self)
        try {
            val answer = stub.answer(invocation)
            log.debug { "Answering(await answering state) ${answer.toStr()} on $invocation" }
            return answer
        } catch (ex: Exception) {
            log.debug { "Throwing(await answering state) ${ex.toStr()} on $invocation" }
            throw ex
        }
    }

    private fun assignFieldIfMockingProperty(mock: Any, matcher: InvocationMatcher, ans: Answer<Any?>) {
        try {
            if (ans !is ConstantAnswer) {
                return
            }
            val methodName = matcher.method.name
            if (!methodName.startsWith("get")) {
                return
            }

            val fieldName = methodName.substring("get".length)
                .toCamelCase()

            InternalPlatformDsl.dynamicSetField(mock, fieldName, ans.constantValue)
        } catch (ex: Exception) {
            log.warn(ex) { "Failed to set backing field (skipping)" }
        }
    }

    private fun String.toCamelCase() = if (isEmpty()) this else substring(0, 1).toLowerCase() + substring(1)
}