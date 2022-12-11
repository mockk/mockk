package io.mockk.impl.recording.states

import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.MockKException
import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CallRound
import io.mockk.impl.recording.CallRoundBuilder
import io.mockk.impl.recording.CommonCallRecorder
import kotlin.math.max
import kotlin.reflect.KClass

abstract class RecordingState(recorder: CommonCallRecorder) : CallRecordingState(recorder) {
    val log = recorder.safeToString(Logger<RecordingState>())

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
            workaroundBoxedNumbers()
            mockPermanently()
        }
    }

    private fun signMatchers() {
        val detector = recorder.factories.signatureMatcherDetector()
        detector.detect(callRounds)

        recorder.calls.clear()
        recorder.calls.addAll(detector.calls)
    }

    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T {
        val signatureValue = recorder.signatureValueGenerator.signatureValue(
            cls,
            recorder.anyValueGenerator,
            recorder.instantiator,
        )

        val packRef: Any = InternalPlatform.packRef(signatureValue)
            ?: error("null packRef for $cls signature $signatureValue")

        builder().addMatcher(matcher, packRef)

        return signatureValue
    }

    protected fun addWasNotCalled(list: List<Any>) {
        builder().addWasNotCalled(list)
    }

    override fun call(invocation: Invocation): Any? {
        val retType = recorder.childHinter.nextChildType { invocation.method.returnType }
        var isTemporaryMock = false
        val temporaryMock: () -> Any = {
            isTemporaryMock = true
            recorder.mockFactory.temporaryMock(retType)
        }
        val retValue = when {
            invocation.method.isToString() -> recorder.stubRepo[invocation.self]?.toStr() ?: ""
            retType in CollectionTypes -> temporaryMock()
            else -> recorder.anyValueGenerator()
                .anyValue(retType, invocation.method.returnTypeNullable, orInstantiateVia = temporaryMock)
        }

        if (retValue == null) {
            isTemporaryMock = false
        }

        builder().addSignedCall(
            retValue,
            isTemporaryMock,
            retType,
            invocation
        )

        return retValue
    }

    private fun callIsNumberUnboxing(call: RecordedCall): Boolean {
        val matcher = call.matcher
        return matcher.self is Number &&
                matcher.method.name.endsWith("Value") &&
                matcher.method.paramTypes.isEmpty()
    }

    private fun workaroundBoxedNumbers() {
        // issue #36
        if (recorder.calls.size == 1) {
            return
        }

        val callsWithoutCasts = recorder.calls.filterNot {
            callIsNumberUnboxing(it)
        }

        if (callsWithoutCasts.size != recorder.calls.size) {
            val callsWithCasts = recorder.calls.filter {
                callIsNumberUnboxing(it)
            }
            log.debug { "Removed ${callsWithCasts.size} unboxing calls:\n${callsWithCasts.joinToString("\n")}" }
        }

        recorder.calls.clear()
        recorder.calls.addAll(callsWithoutCasts)
    }

    fun mockPermanently() {
        val mocker = recorder.factories.permanentMocker()

        val resultCalls = mocker.mock(recorder.calls)

        recorder.calls.clear()
        recorder.calls.addAll(resultCalls)
    }

    override fun nCalls(): Int = callRoundBuilder?.signedCalls?.size ?: 0

    override fun isLastCallReturnsNothing(): Boolean {
        val lastCall = callRoundBuilder?.signedCalls?.lastOrNull()
            ?: return false

        return lastCall.method.returnsNothing
    }

    /**
     * Main idea is to have enough random information
     * to create signature for the argument.
     *
     * Max 40 calls looks like reasonable compromise
     */
    @Suppress("DEPRECATION_ERROR")
    override fun estimateCallRounds(): Int {
        val regularArguments = builder().signedCalls
            .flatMap { it.args }
            .filterNotNull()
            .map(this::typeEstimation)
            .maxOfOrNull { it } ?: 1

        val varargArguments = builder().signedCalls
            .mapNotNull {
                if (it.method.varArgsArg != -1) {
                    it.method.paramTypes[it.method.varArgsArg]
                } else {
                    null
                }
            }.map(this::varArgTypeEstimation)
            .maxOfOrNull { it } ?: 1

        return max(regularArguments, varargArguments)
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

    private fun varArgTypeEstimation(it: KClass<*>): Int {
        return when (it) {
            BooleanArray::class -> 40
            ByteArray::class -> 8
            CharArray::class -> 4
            ShortArray::class -> 4
            IntArray::class -> 2
            FloatArray::class -> 2
            else -> 1
        }
    }

    override fun discardLastCallRound() {
        callRoundBuilder = null
    }

    private fun builder(): CallRoundBuilder = callRoundBuilder
        ?: throw MockKException("Call builder is not initialized. Bad state")

    private companion object {
        private val CollectionTypes = listOf(
            List::class,
            Map::class,
            Set::class,
            ArrayList::class,
            HashMap::class,
            HashSet::class
        )
    }
}
