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
import io.mockk.proxy.MockKAgentFactory
import io.mockk.proxy.MockKAgentLogFactory
import java.util.*

class JvmMockKGateway : MockKGateway {
    val safeLog: SafeLog = SafeLog({ callRecorderTL.get() })

    val instanceFactoryRegistryIntrnl = CommonInstanceFactoryRegistry()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    val agentFactory: MockKAgentFactory = if (InternalPlatform.isRunningAndroidInstrumentationTest())
        InternalPlatform.loadPlugin(
            "io.mockk.proxy.android.AndroidMockKAgentFactory",
            "Android instrumented test is running, " +
                    "include 'io.mockk:mockk-andorid' dependency " +
                    "instead 'io.mockk:mockk'"
        )
    else
        InternalPlatform.loadPlugin(
            "io.mockk.proxy.jvm.JvmMockKAgentFactory",
            "Check if you included 'io.mockk:mockk-andorid' dependency " +
                    "instead of 'io.mockk:mockk'"
        )

    init {
        agentFactory.init(object : MockKAgentLogFactory {
            override fun logger(cls: Class<*>) = Logger.loggerFactory(cls.kotlin).adaptor()
        })
    }

    val stubRepo = StubRepository(safeLog)

    val instantiator = JvmInstantiator(
        agentFactory.instantiator,
        instanceFactoryRegistryIntrnl
    )

    val anyValueGenerator = JvmAnyValueGenerator(instantiator)
    val signatureValueGenerator = JvmSignatureValueGenerator(Random())


    val gatewayAccess = StubGatewayAccess({ callRecorder }, anyValueGenerator, stubRepo, safeLog)

    override val mockFactory: AbstractMockFactory = JvmMockFactory(
        agentFactory.proxyMaker,
        instantiator,
        stubRepo,
        gatewayAccess
    )

    override val clearer = CommonClearer(stubRepo, safeLog)

    override val staticMockFactory = JvmStaticMockFactory(
        agentFactory.staticProxyMaker,
        stubRepo,
        gatewayAccess
    )

    override val objectMockFactory = JvmObjectMockFactory(
        agentFactory.proxyMaker,
        stubRepo,
        gatewayAccess
    )

    override val constructorMockFactory = JvmConstructorMockFactory(
        agentFactory.constructorProxyMaker,
        clearer,
        mockFactory,
        agentFactory.proxyMaker,
        gatewayAccess.copy(mockFactory = mockFactory)
    )


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
                val runningAndroid = InternalPlatform.isRunningAndroidInstrumentationTest()
                "Starting JVM MockK implementation. " +
                        (if (runningAndroid) "Android instrumented test detected. " else "") +
                        "Java version = ${System.getProperty("java.version")}. "
            }
        }

        val defaultImplementation = JvmMockKGateway()
        val defaultImplementationBuilder = { defaultImplementation }
    }

}
