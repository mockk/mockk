package io.mockk.impl

import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.Ref
import io.mockk.common.*
import kotlin.reflect.KClass

class JsMockKGateway : MockKGateway {
    val instanceFactoryRegistryIntrnl = InstanceFactoryRegistryImpl()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    val stubRepo = StubRepository()
    val instantiator = JsInstantiator(instanceFactoryRegistryIntrnl)
    val anyValueGenerator = AnyValueGenerator()
    val signatureValueGenerator = JsSignatureValueGenerator()


    override val mockFactory: MockFactory = JsMockFactory(
            stubRepo,
            instantiator)

    override val staticMockFactory: StaticMockFactory
        get() = throw UnsupportedOperationException("Static mocks are not supported in JS version")

    override val clearer = CommonClearer(stubRepo)

    val unorderedVerifier = UnorderedCallVerifier(stubRepo)
    val allVerifier = AllCallsCallVerifier(stubRepo)
    val orderedVerifier = OrderedCallVerifierImpl(stubRepo)
    val sequenceVerifier = SequenceCallVerifierImpl(stubRepo)

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

    override val callRecorder: CallRecorder = CallRecorderImpl(
            stubRepo,
            instantiator,
            signatureValueGenerator,
            mockFactory,
            anyValueGenerator,
            callRecorderFactories)

    override val stubbingRecorder: Stubber = StubberImpl({ callRecorder }, ::AutoHinter)
    override val verifyingRecorder: Verifier = VerifierImpl({ callRecorder }, stubRepo, ::AutoHinter)

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


