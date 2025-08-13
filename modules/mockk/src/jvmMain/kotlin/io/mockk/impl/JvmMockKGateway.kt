package io.mockk.impl

import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.impl.annotations.JvmMockInitializer
import io.mockk.impl.eval.EveryBlockEvaluator
import io.mockk.impl.eval.ExcludeBlockEvaluator
import io.mockk.impl.eval.VerifyBlockEvaluator
import io.mockk.impl.instantiation.*
import io.mockk.impl.log.JvmLogging
import io.mockk.impl.log.JvmLogging.adaptor
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeToString
import io.mockk.impl.recording.*
import io.mockk.impl.recording.states.*
import io.mockk.impl.stub.CommonClearer
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.*
import io.mockk.proxy.MockKAgentFactory
import io.mockk.proxy.MockKAgentLogFactory
import java.util.Random

class JvmMockKGateway : MockKGateway {
    val safeToString: SafeToString = SafeToString { callRecorderTL.get() }

    private val instanceFactoryRegistryIntrnl = CommonInstanceFactoryRegistry()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    private val agentFactory: MockKAgentFactory = if (InternalPlatform.isRunningAndroidInstrumentationTest())
        InternalPlatform.loadPlugin(
            "io.mockk.proxy.android.AndroidMockKAgentFactory",
            "Android instrumented test is running, " +
                    "include 'io.mockk:mockk-android' dependency " +
                    "instead 'io.mockk:mockk'"
        )
    else
        InternalPlatform.loadPlugin(
            "io.mockk.proxy.jvm.JvmMockKAgentFactory",
            "Check if you included 'io.mockk:mockk-android' dependency " +
                    "instead of 'io.mockk:mockk'"
        )

    init {
        agentFactory.init(object : MockKAgentLogFactory {
            override fun logger(cls: Class<*>) = Logger.loggerFactory(cls.kotlin).adaptor()
        })
    }

    val stubRepo = StubRepository(safeToString)

    val instantiator = JvmInstantiator(
        agentFactory.instantiator,
        instanceFactoryRegistryIntrnl
    )

    val anyValueGeneratorProvider: () -> AnyValueGenerator = {
        if (anyValueGenerator == null) {
            anyValueGenerator = anyValueGeneratorFactory.invoke(instantiator.instantiate(Void::class))
        }
        anyValueGenerator!!
    }
    val signatureValueGenerator = JvmSignatureValueGenerator(Random())


    val gatewayAccess = StubGatewayAccess({ callRecorder }, anyValueGeneratorProvider, stubRepo, safeToString)

    override val mockFactory: AbstractMockFactory = JvmMockFactory(
        agentFactory.proxyMaker,
        instantiator,
        stubRepo,
        gatewayAccess
    )

    val gatewayAccessWithFactory = gatewayAccess.copy(mockFactory = mockFactory)

    override val clearer = CommonClearer(stubRepo, safeToString)

    override val staticMockFactory = JvmStaticMockFactory(
        agentFactory.staticProxyMaker,
        stubRepo,
        gatewayAccessWithFactory
    )

    override val objectMockFactory = JvmObjectMockFactory(
        agentFactory.proxyMaker,
        stubRepo,
        gatewayAccessWithFactory
    )

    override val constructorMockFactory = JvmConstructorMockFactory(
        agentFactory.constructorProxyMaker,
        clearer,
        mockFactory,
        agentFactory.proxyMaker,
        gatewayAccessWithFactory
    )

    override val mockTypeChecker = JvmMockTypeChecker(
        stubRepo
    ) {
        constructorMockFactory.isMock(it)
    }

    override fun verifier(params: VerificationParameters): CallVerifier {

        val verifier = when (params.ordering) {
            Ordering.UNORDERED -> UnorderedCallVerifier(stubRepo, safeToString)
            Ordering.ALL -> AllCallsCallVerifier(stubRepo, safeToString)
            Ordering.ORDERED -> OrderedCallVerifier(stubRepo, safeToString)
            Ordering.SEQUENCE -> SequenceCallVerifier(stubRepo, safeToString)
        }

        return if (params.timeout > 0) {
            TimeoutVerifier(stubRepo, verifier)
        } else {
            verifier
        }
    }


    val callRecorderFactories = CallRecorderFactories(
        { SignatureMatcherDetector(safeToString) { ChainedCallDetector(safeToString) } },
        { CallRoundBuilder(safeToString) },
        ::ChildHinter,
        this::verifier,
        { PermanentMocker(stubRepo, safeToString) },
        ::VerificationCallSorter,
        ::AnsweringState,
        ::StubbingState,
        ::VerifyingState,
        ::ExclusionState,
        ::StubbingAwaitingAnswerState,
        ::SafeLoggingState
    )

    private val callRecorderTL = object : ThreadLocal<CommonCallRecorder>() {
        override fun initialValue(): CommonCallRecorder = CommonCallRecorder(
            stubRepo,
            instantiator,
            signatureValueGenerator,
            mockFactory,
            anyValueGeneratorProvider,
            safeToString,
            callRecorderFactories,
            { recorder -> callRecorderFactories.answeringState(recorder) },
            verificationAcknowledger
        )
    }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()

    override val stubber: Stubber = EveryBlockEvaluator(callRecorderTL::get, ::JvmAutoHinter)
    override val verifier: Verifier = VerifyBlockEvaluator(callRecorderTL::get, stubRepo, ::JvmAutoHinter)
    override val excluder: Excluder = ExcludeBlockEvaluator(callRecorderTL::get, stubRepo, ::JvmAutoHinter)
    override val mockInitializer = JvmMockInitializer(this)
    override val verificationAcknowledger = CommonVerificationAcknowledger(stubRepo, safeToString)

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

        private var anyValueGenerator: AnyValueGenerator? = null
        var anyValueGeneratorFactory: (voidInstance: Any) -> JvmAnyValueGenerator =
            { voidInstance -> JvmAnyValueGenerator(voidInstance) }
            set(value) {
                anyValueGenerator = null
                field = value
            }

        val defaultImplementation = JvmMockKGateway()
        val defaultImplementationBuilder = { defaultImplementation }
    }

}
