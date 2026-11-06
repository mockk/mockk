package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.ConstantAnswer
import io.mockk.InternalPlatformDsl
import io.mockk.InvocationMatcher
import io.mockk.MockKGateway
import io.mockk.MockKSettings
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.AnswerAnsweringOpportunity
import io.mockk.impl.stub.MockKStub
import io.mockk.impl.stub.Stub

class StubbingAwaitingAnswerState(
    recorder: CommonCallRecorder,
) : CallRecordingState(recorder) {
    val log = recorder.safeToString(Logger<StubbingAwaitingAnswerState>())

    override fun answerOpportunity(): MockKGateway.AnswerOpportunity<*> {
        val calls = recorder.calls

        var answerOpportunity: AnswerAnsweringOpportunity<*>? = null

        for ((idx, recordedCall) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val ans =
                if (lastCall) {
                    answerOpportunity =
                        AnswerAnsweringOpportunity<Any> {
                            recorder.safeExec { recordedCall.matcher.toString() }
                        }
                    answerOpportunity
                } else if (recordedCall.isRetValueMock) {
                    ConstantAnswer(recordedCall.retValue)
                } else {
                    continue
                }

            val mock = recordedCall.matcher.self
            val stub = recorder.stubRepo.stubFor(mock)

            stub.addAnswer(recordedCall.matcher, ans)

            if (stub::class == MockKStub::class) {
                assignFieldIfMockingProperty(stub, mock, recordedCall.matcher, ans)
            }
        }

        calls.clear()

        log.trace { "Done stubbing. Still accepting additional answers" }

        recorder.state = recorder.factories.answeringState(recorder)
        return answerOpportunity!!
    }

    private fun assignFieldIfMockingProperty(
        stub: Stub,
        mock: Any,
        matcher: InvocationMatcher,
        ans: Answer<Any?>,
    ) {
        if (ans is AnswerAnsweringOpportunity<*>) {
            ans.onFirstAnswer { answer ->
                assignFieldIfMockingProperty(stub, mock, matcher, answer)
            }
            return
        }
        try {
            if (ans !is ConstantAnswer) {
                return
            }
            val methodName = matcher.method.name
            if (!methodName.startsWith("get")) {
                return
            }

            val fieldName =
                methodName
                    .substring("get".length)
                    .toCamelCase()

            InternalPlatformDsl.dynamicSetField(mock, fieldName, ans.constantValue)
            // The stub affected behavior through the backing field without the getter being
            // intercepted, so count it as used to keep checkUnnecessaryStub accurate (#1345).
            stub.markAnswerUsed(matcher)
        } catch (ex: Exception) {
            if (MockKSettings.failOnSetBackingFieldException) {
                log.error(ex) { "Failed to set backing field" }
                throw ex
            } else {
                log.warn(ex) { "Failed to set backing field (skipping)" }
            }
        }
    }

    private fun String.toCamelCase() = if (isEmpty()) this else substring(0, 1).lowercase() + substring(1)
}
