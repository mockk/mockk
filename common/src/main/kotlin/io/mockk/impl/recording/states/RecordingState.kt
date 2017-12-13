package io.mockk.impl.recording.states

import io.mockk.impl.InternalPlatform
import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.MockKException
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.*
import kotlin.reflect.KClass

abstract class RecordingState(recorder: CommonCallRecorder) : CallRecordingState(recorder) {
    val log = recorder.safeLog(Logger<RecordingState>())

    private var callRoundBuilder: CallRoundBuilder? = null
    private val callRounds = mutableListOf<CallRound>()

    override fun round(round: Int, total: Int) {
        val builder = callRoundBuilder
        if (builder != null) {
            callRounds.add(builder.build())
        }

        callRoundBuilder = recorder.factories.callRoundBuilder()
        recorder.childHinter = recorder.factories.childHinter()

        if (round == total) {
            signMatchers()
            mockPermanently()
        }
    }

    private fun signMatchers() {
        val detector = recorder.factories.signatureMatcherDetector()
        detector.detect(callRounds)

        recorder.calls.clear()
        recorder.calls.addAll(detector.calls)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T {
        val signatureValue = recorder.signatureValueGenerator.signatureValue(cls) {
            recorder.anyValueGenerator.anyValue(cls) {
                recorder.instantiator.instantiate(cls)
            } as T
        }

        builder().addMatcher(matcher, InternalPlatform.packRef(signatureValue)!!)

        return signatureValue
    }

    protected fun addWasNotCalled(list: List<Any>) {
        builder().addWasNotCalled(list)
    }

    override fun call(invocation: Invocation): Any? {
        val retType = recorder.childHinter.nextChildType { invocation.method.returnType }
        var isTemporaryMock = false

        val retValue = recorder.anyValueGenerator.anyValue(retType) {
            isTemporaryMock = true
            recorder.mockFactory.temporaryMock(retType)
        }

        if (retValue == null) {
            isTemporaryMock = false
        }

        builder().addSignedCall(
                retValue,
                isTemporaryMock,
                retType,
                invocation)

        return retValue
    }

    fun mockPermanently() {
        val mocker = recorder.factories.permanentMocker()

        val resultCalls = mocker.mock(recorder.calls)

        recorder.calls.clear()
        recorder.calls.addAll(resultCalls)

        log.trace { "Mocked permanently" }
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
                .flatMap { it.args }
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