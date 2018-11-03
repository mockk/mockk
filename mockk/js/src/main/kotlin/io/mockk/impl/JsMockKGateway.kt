package io.mockk.impl

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.impl.eval.EveryBlockEvaluator
import io.mockk.impl.eval.VerifyBlockEvaluator
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.impl.instantiation.CommonInstanceFactoryRegistry
import io.mockk.impl.instantiation.JsInstantiator
import io.mockk.impl.instantiation.JsMockFactory
import io.mockk.impl.log.JsConsoleLogger
import io.mockk.impl.log.Logger
import io.mockk.impl.log.safeToString
import io.mockk.impl.recording.*
import io.mockk.impl.recording.states.*
import io.mockk.impl.stub.CommonClearer
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.AllCallsCallVerifier
import io.mockk.impl.verify.OrderedCallVerifier
import io.mockk.impl.verify.SequenceCallVerifier
import io.mockk.impl.verify.UnorderedCallVerifier
import kotlin.reflect.KClass

class JsMockKGateway : MockKGateway {
    val safeToString = safeToString({ commonCallRecorder })

    val instanceFactoryRegistryIntrnl = CommonInstanceFactoryRegistry()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    val stubRepo = StubRepository(safeToString)
    val instantiator = JsInstantiator(instanceFactoryRegistryIntrnl)
    val anyValueGenerator = AnyValueGenerator()
    val signatureValueGenerator = JsSignatureValueGenerator()


    override val mockFactory: MockFactory = JsMockFactory(
        stubRepo,
        instantiator,
        StubGatewayAccess({ callRecorder }, anyValueGenerator, stubRepo, safeToString)
    )

    override val staticMockFactory: StaticMockFactory
        get() = throw UnsupportedOperationException("Static mocks are not supported in JS version")

    override val objectMockFactory: ObjectMockFactory
        get() = throw UnsupportedOperationException("Object mocks are not supported in JS version")

    override val clearer = CommonClearer(stubRepo, safeToString)

    val unorderedVerifier = UnorderedCallVerifier(stubRepo, safeToString)
    val allVerifier = AllCallsCallVerifier(stubRepo, safeToString)
    val orderedVerifier = OrderedCallVerifier(stubRepo, safeToString)
    val sequenceVerifier = SequenceCallVerifier(stubRepo, safeToString)

    override fun verifier(ordering: Ordering): CallVerifier =
        when (ordering) {
            Ordering.UNORDERED -> unorderedVerifier
            Ordering.ALL -> allVerifier
            Ordering.ORDERED -> orderedVerifier
            Ordering.SEQUENCE -> sequenceVerifier
        }

    val callRecorderFactories = CallRecorderFactories(
        { SignatureMatcherDetector({ ChainedCallDetector(safeToString) }) },
        { CallRoundBuilder(safeToString) },
        ::ChildHinter,
        this::verifier,
        { PermanentMocker(stubRepo, safeToString) },
        ::VerificationCallSorter,
        ::AnsweringState,
        ::AnsweringStillAcceptingAnswersState,
        ::StubbingState,
        ::VerifyingState,
        ::StubbingAwaitingAnswerState,
        ::SafeLoggingState
    )

    val commonCallRecorder: CommonCallRecorder = CommonCallRecorder(
        stubRepo,
        instantiator,
        signatureValueGenerator,
        mockFactory,
        anyValueGenerator,
        safeToString,
        callRecorderFactories,
        { recorder -> callRecorderFactories.answeringState(recorder) })
    override val callRecorder: CallRecorder = commonCallRecorder

    override val stubber: Stubber = EveryBlockEvaluator({ callRecorder }, ::AutoHinter)
    override val verifier: Verifier = VerifyBlockEvaluator({ callRecorder }, stubRepo, ::AutoHinter)
    override val mockInitializer: MockInitializer
        get() = throw MockKException("MockK annotations are not supported in JS")

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
    }
}


