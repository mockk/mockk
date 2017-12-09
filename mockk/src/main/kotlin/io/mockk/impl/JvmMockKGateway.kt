package io.mockk.impl

import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.Ref
import io.mockk.impl.verify.AllCallsCallVerifier
import io.mockk.impl.stub.CommonClearer
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.UnorderedCallVerifier
import io.mockk.impl.eval.EveryBlockEvaluator
import io.mockk.impl.eval.VerifyBlockEvaluator
import io.mockk.impl.instantiation.*
import io.mockk.impl.log.JvmLogging
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.*
import io.mockk.impl.recording.states.AnsweringCallRecorderState
import io.mockk.impl.recording.states.StubbingAwaitingAnswerCallRecorderState
import io.mockk.impl.recording.states.StubbingCallRecorderState
import io.mockk.impl.recording.states.VerifyingCallRecorderState
import io.mockk.impl.verify.OrderedCallVerifier
import io.mockk.impl.verify.SequenceCallVerifier
import io.mockk.impl.log.JvmLogging.adaptor
import io.mockk.proxy.MockKInstrumentation
import io.mockk.proxy.MockKInstrumentationLoader
import io.mockk.proxy.MockKProxyMaker
import java.util.*

class JvmMockKGateway : MockKGateway {
    val instanceFactoryRegistryIntrnl = CommonInstanceFactoryRegistry()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    val stubRepo = StubRepository()
    val instantiator = JvmInstantiator(MockKProxyMaker.INSTANCE, instanceFactoryRegistryIntrnl)
    val anyValueGenerator = JvmAnyValueGenerator()
    val signatureValueGenerator = JvmSignatureValueGenerator(Random())


    override val mockFactory: MockFactory = JvmMockFactory(
            MockKProxyMaker.INSTANCE,
            instantiator,
            stubRepo)

    override val staticMockFactory = JvmStaticMockFactory(
            MockKProxyMaker.INSTANCE,
            stubRepo)

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

    private val callRecorderTL = object : ThreadLocal<CommonCallRecorder>() {
        override fun initialValue() = CommonCallRecorder(
                stubRepo,
                instantiator,
                signatureValueGenerator,
                mockFactory,
                anyValueGenerator,
                callRecorderFactories)
    }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()

    override val stubber: Stubber = EveryBlockEvaluator(callRecorderTL::get, ::JvmAutoHinter)
    override val verifier: Verifier = VerifyBlockEvaluator(callRecorderTL::get, stubRepo, ::JvmAutoHinter)

    companion object {
        private var log: Logger

        init {
            Logger.loggerFactory = JvmLogging.slf4jOrJulLogging()

            log = Logger<JvmMockKGateway>()

            log.trace {
                "Starting Java MockK implementation. " +
                        "Java version = ${System.getProperty("java.version")}. "
            }

            MockKProxyMaker.log = Logger<MockKProxyMaker>().adaptor()
            MockKInstrumentationLoader.log = Logger<MockKInstrumentationLoader>().adaptor()
            MockKInstrumentation.log = Logger<MockKInstrumentation>().adaptor()

            MockKInstrumentation.init()
        }

        val defaultImplementation = JvmMockKGateway()
        val defaultImplementationBuilder = { defaultImplementation }

        inline fun <T> useImpl(block: () -> T): T {
            MockKGateway.implementation = defaultImplementationBuilder
            return block()
        }
    }

}


