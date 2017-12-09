package io.mockk.impl

import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.Ref
import io.mockk.impl.eval.EveryBlockEvaluator
import io.mockk.impl.eval.VerifyBlockEvaluator
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.impl.instantiation.CommonInstanceFactoryRegistry
import io.mockk.impl.instantiation.JsInstantiator
import io.mockk.impl.instantiation.JsMockFactory
import io.mockk.impl.log.JsConsoleLogger
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.*
import io.mockk.impl.recording.states.AnsweringCallRecorderState
import io.mockk.impl.recording.states.StubbingAwaitingAnswerCallRecorderState
import io.mockk.impl.recording.states.StubbingCallRecorderState
import io.mockk.impl.recording.states.VerifyingCallRecorderState
import io.mockk.impl.stub.CommonClearer
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.AllCallsCallVerifier
import io.mockk.impl.verify.OrderedCallVerifier
import io.mockk.impl.verify.SequenceCallVerifier
import io.mockk.impl.verify.UnorderedCallVerifier
import kotlin.reflect.KClass

class JsMockKGateway : MockKGateway {
    val instanceFactoryRegistryIntrnl = CommonInstanceFactoryRegistry()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    val stubRepo = StubRepository()
    val instantiator = JsInstantiator(instanceFactoryRegistryIntrnl)
    val anyValueGenerator = AnyValueGenerator()
    val signatureValueGenerator = JsSignatureValueGenerator()


    override val mockFactory: MockFactory = JsMockFactory(
            stubRepo,
            instantiator,
            anyValueGenerator)

    override val staticMockFactory: StaticMockFactory
        get() = throw UnsupportedOperationException("Static mocks are not supported in JS version")

    override val clearer = CommonClearer(stubRepo)

    val unorderedVerifier = UnorderedCallVerifier(stubRepo)
    val allVerifier = AllCallsCallVerifier(stubRepo)
    val orderedVerifier = OrderedCallVerifier(stubRepo)
    val sequenceVerifier = SequenceCallVerifier(stubRepo)

    override fun verifier(ordering: Ordering): CallVerifier =
            when (ordering) {
                Ordering.UNORDERED -> unorderedVerifier
                Ordering.ALL -> allVerifier
                Ordering.ORDERED -> orderedVerifier
                Ordering.SEQUENCE -> sequenceVerifier
            }

    fun signatureMatcherDetectorFactory(callRounds: List<CallRound>, mocks: List<Ref>): SignatureMatcherDetector {
        return SignatureMatcherDetector(callRounds, mocks, ::ChainedCallDetector)
    }

    val callRecorderFactories = CallRecorderFactories(
            this::signatureMatcherDetectorFactory,
            ::CallRoundBuilder,
            ::ChildHinter,
            this::verifier,
            ::AnsweringCallRecorderState,
            ::StubbingCallRecorderState,
            ::VerifyingCallRecorderState,
            ::StubbingAwaitingAnswerCallRecorderState)

    override val callRecorder: CallRecorder = CommonCallRecorder(
            stubRepo,
            instantiator,
            signatureValueGenerator,
            mockFactory,
            anyValueGenerator,
            callRecorderFactories)

    override val stubber: Stubber = EveryBlockEvaluator({ callRecorder }, ::AutoHinter)
    override val verifier: Verifier = VerifyBlockEvaluator({ callRecorder }, stubRepo, ::AutoHinter)

    companion object {
        private var log: Logger

        init {
            Logger.loggerFactory = { cls: KClass<*> -> JsConsoleLogger(cls) }

            log = Logger<JsMockKGateway>()

            log.trace {
                "Starting JavaScript MockK implementation. "
            }
        }

        val defaultImplementation = JsMockKGateway()
        val defaultImplementationBuilder = { defaultImplementation }

        inline fun <T> useImpl(block: () -> T): T {
            MockKGateway.implementation = defaultImplementationBuilder
            return block()
        }
    }

}


