package io.mockk.jvm

import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.Ref
import io.mockk.common.AllCallsCallVerifier
import io.mockk.common.StubRepository
import io.mockk.common.UnorderedCallVerifier
import io.mockk.impl.*
import io.mockk.jvm.JvmLogging.adaptor
import io.mockk.proxy.MockKInstrumentation
import io.mockk.proxy.MockKInstrumentationLoader
import io.mockk.proxy.MockKProxyMaker
import java.util.*

class JvmMockKGateway : MockKGateway {
    val instanceFactoryRegistryIntrnl = InstanceFactoryRegistryImpl()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    val stubRepo = StubRepository()
    val instantiator = JvmInstantiator(MockKProxyMaker.INSTANCE, instanceFactoryRegistryIntrnl)
    val anyValueGenerator = JvmAnyValueGenerator()
    val signatureValueGenerator = JvmSignatureValueGenerator(Random())


    override val mockFactory: MockFactory = JvmMockFactory(
            MockKProxyMaker.INSTANCE,
            instantiator,
            stubRepo)

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

    private val callRecorderTL = object : ThreadLocal<CallRecorderImpl>() {
        override fun initialValue() = CallRecorderImpl(
                stubRepo,
                instantiator,
                signatureValueGenerator,
                mockFactory,
                anyValueGenerator,
                callRecorderFactories)
    }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()

    override val stubbingRecorder: Stubber = StubberImpl(callRecorderTL::get, ::JvmAutoHinter)
    override val verifyingRecorder: Verifier = VerifierImpl(callRecorderTL::get, stubRepo, ::JvmAutoHinter)

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


