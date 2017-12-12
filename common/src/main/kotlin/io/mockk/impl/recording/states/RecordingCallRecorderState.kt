package io.mockk.impl.recording.states

import io.mockk.impl.InternalPlatform
import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.MockKException
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.*
import kotlin.reflect.KClass

abstract class RecordingCallRecorderState(recorder: CommonCallRecorder) : CallRecorderState(recorder) {
    val log = recorder.safeLog(Logger<RecordingCallRecorderState>())

    private var callRoundBuilder: CallRoundBuilder? = null
    private val callRounds = mutableListOf<CallRound>()
    val childMocks = TemporaryMocks()

    override fun round(round: Int, total: Int) {
        val builder = callRoundBuilder
        if (builder != null) {
            callRounds.add(builder.build())
        }

        callRoundBuilder = recorder.factories.callRoundBuilder()
        recorder.childHinter = recorder.factories.childHinter()

        if (round == total) {
            signMatchers()
            mockRealChilds()
        }
    }

    private fun signMatchers() {
        recorder.calls.clear()
        val detector = recorder.factories.signatureMatcherDetector()
        val calls = detector.detect(callRounds, childMocks.mocks)
        recorder.calls.addAll(calls)
    }

    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T {
        val signatureValue = recorder.signatureValueGenerator.signatureValue(cls) {
            recorder.anyValueGenerator.anyValue(cls) {
                recorder.instantiator.instantiate(cls)
            } as T
        }

        builder().addMatcher(matcher, InternalPlatform.packRef(signatureValue)!!)

        return signatureValue
    }

    override fun call(invocation: Invocation): Any? {
        childMocks.requireNoArgIsChildMock(invocation.args)

        val retType = recorder.childHinter.nextChildType { invocation.method.returnType }

        builder().addSignedCall(retType, invocation)

        return recorder.anyValueGenerator.anyValue(retType) {
            childMocks.childMock(retType) {
                recorder.mockFactory.temporaryMock(retType)
            }
        }
    }

    fun mockRealChilds() {
        val mocker = recorder.factories.realChildMocker()

        val resultCalls = mocker.mock(recorder.calls)

        recorder.calls.clear()
        recorder.calls.addAll(resultCalls)

        log.trace { "Mocked childs" }
    }

    override fun nCalls(): Int = callRoundBuilder?.signedCalls?.size ?: 0

    /**
     * Main idea is to have enough random information
     * to create signature for the argument.
     *
     * Max 40 calls looks like reasonable compromise
     */
    override fun estimateCallRounds(): Int {
        return builder().signedCalls
                .flatMap { it.invocation.args }
                .filterNotNull()
                .map(this::typeEstimation)
                .max() ?: 1
    }

    private fun typeEstimation(it: Any): Int {
        return when (it::class) {
            Boolean::class -> 40
            Byte::class -> 8
            Char::class -> 4
            Short::class -> 4
            Int::class -> 2
            Float::class -> 2
            else -> 1
        }
    }

    override fun discardLastCallRound() {
        callRoundBuilder = null
    }

    private fun builder(): CallRoundBuilder = callRoundBuilder
            ?: throw MockKException("Call builder is not initialized. Bad state")

}