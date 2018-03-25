package io.mockk.impl

import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.impl.annotations.JvmMockInitializer
import io.mockk.impl.eval.EveryBlockEvaluator
import io.mockk.impl.eval.VerifyBlockEvaluator
import io.mockk.impl.instantiation.*
import io.mockk.impl.log.JvmLogging
import io.mockk.impl.log.JvmLogging.adaptor
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeLog
import io.mockk.impl.recording.*
import io.mockk.impl.recording.states.*
import io.mockk.impl.stub.CommonClearer
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.AllCallsCallVerifier
import io.mockk.impl.verify.OrderedCallVerifier
import io.mockk.impl.verify.SequenceCallVerifier
import io.mockk.impl.verify.UnorderedCallVerifier
import io.mockk.mockk
import io.mockk.proxy.MockKInstrumentation
import io.mockk.proxy.MockKInstrumentationLoader
import io.mockk.proxy.MockKProxyMaker
import java.util.*
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

class JvmMockKGateway : MockKGateway {
    val safeLog: SafeLog = SafeLog({ callRecorderTL.get() })

    val instanceFactoryRegistryIntrnl = CommonInstanceFactoryRegistry()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    val stubRepo = StubRepository(safeLog)
    val instantiator = JvmInstantiator(MockKProxyMaker.INSTANCE, instanceFactoryRegistryIntrnl)
    val anyValueGenerator = JvmAnyValueGenerator()
    val signatureValueGenerator = JvmSignatureValueGenerator(Random())


    override val mockFactory: MockFactory = JvmMockFactory(
        MockKProxyMaker.INSTANCE,
        instantiator,
        stubRepo,
        StubGatewayAccess({ callRecorder }, anyValueGenerator, stubRepo, safeLog)
    )

    override val staticMockFactory = JvmStaticMockFactory(
        MockKProxyMaker.INSTANCE,
        stubRepo,
        StubGatewayAccess({ callRecorder }, anyValueGenerator, stubRepo, safeLog, mockFactory)
    )

    override val objectMockFactory = JvmObjectMockFactory(
        MockKProxyMaker.INSTANCE,
        stubRepo,
        StubGatewayAccess({ callRecorder }, anyValueGenerator, stubRepo, safeLog, mockFactory)
    )

    override val clearer = CommonClearer(stubRepo, safeLog)

    val unorderedVerifier = UnorderedCallVerifier(stubRepo, safeLog)
    val allVerifier = AllCallsCallVerifier(stubRepo, safeLog)
    val orderedVerifier = OrderedCallVerifier(stubRepo, safeLog)
    val sequenceVerifier = SequenceCallVerifier(stubRepo, safeLog)

    override fun verifier(ordering: Ordering): CallVerifier =
        when (ordering) {
            Ordering.UNORDERED -> unorderedVerifier
            Ordering.ALL -> allVerifier
            Ordering.ORDERED -> orderedVerifier
            Ordering.SEQUENCE -> sequenceVerifier
        }

    val callRecorderFactories = CallRecorderFactories(
        { SignatureMatcherDetector({ ChainedCallDetector(safeLog) }) },
        { CallRoundBuilder(safeLog) },
        ::ChildHinter,
        this::verifier,
        { PermanentMocker(stubRepo, safeLog) },
        ::VerificationCallSorter,
        ::AnsweringState,
        ::AnsweringStillAcceptingAnswersState,
        ::StubbingState,
        ::VerifyingState,
        ::StubbingAwaitingAnswerState,
        ::SafeLoggingState
    )

    private val callRecorderTL = object : ThreadLocal<CommonCallRecorder>() {
        override fun initialValue(): CommonCallRecorder = CommonCallRecorder(
            stubRepo,
            instantiator,
            signatureValueGenerator,
            mockFactory,
            anyValueGenerator,
            safeLog,
            callRecorderFactories,
            { recorder -> callRecorderFactories.answeringState(recorder) })
    }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()

    override val stubber: Stubber = EveryBlockEvaluator(callRecorderTL::get, ::JvmAutoHinter)
    override val verifier: Verifier = VerifyBlockEvaluator(callRecorderTL::get, stubRepo, ::JvmAutoHinter)
    override val mockInitializer = JvmMockInitializer(this)

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
    }

}
