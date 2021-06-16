package io.mockk.impl

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.impl.eval.EveryBlockEvaluator
import io.mockk.impl.eval.ExcludeBlockEvaluator
import io.mockk.impl.eval.VerifyBlockEvaluator
import io.mockk.impl.instantiation.*
import io.mockk.impl.log.JsConsoleLogger
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeToString
import io.mockk.impl.recording.*
import io.mockk.impl.recording.states.*
import io.mockk.impl.stub.CommonClearer
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.*
import kotlin.reflect.KClass

class JsMockKGateway : MockKGateway {
    val safeToString = SafeToString({ commonCallRecorder })

    val instanceFactoryRegistryIntrnl = CommonInstanceFactoryRegistry()
    override val instanceFactoryRegistry: InstanceFactoryRegistry = instanceFactoryRegistryIntrnl

    val stubRepo = StubRepository(safeToString)
    val instantiator = JsInstantiator(instanceFactoryRegistryIntrnl)
    val anyValueGenerator = AnyValueGenerator()
    val signatureValueGenerator = JsSignatureValueGenerator()


    override val mockFactory: MockFactory = JsMockFactory(
        stubRepo,
        instantiator,
        StubGatewayAccess({ callRecorder }, { anyValueGenerator }, stubRepo, safeToString)
    )

    override val clearer = CommonClearer(stubRepo, safeToString)

    override val staticMockFactory: StaticMockFactory
        get() = throw UnsupportedOperationException("Static mocks are not supported in JS version")

    override val objectMockFactory: ObjectMockFactory
        get() = throw UnsupportedOperationException("Object mocks are not supported in JS version")

    override val constructorMockFactory: ConstructorMockFactory
        get() = throw UnsupportedOperationException("Constructor mocks are not supported in JS version")

    override val mockTypeChecker = CommonMockTypeChecker(stubRepo) { false }

    override fun verifier(params: VerificationParameters): CallVerifier {
        val ordering = params.ordering

        val verifier = when (ordering) {
            Ordering.UNORDERED -> UnorderedCallVerifier(stubRepo, safeToString)
            Ordering.ALL -> AllCallsCallVerifier(stubRepo, safeToString)
            Ordering.ORDERED -> OrderedCallVerifier(stubRepo, safeToString)
            Ordering.SEQUENCE -> SequenceCallVerifier(stubRepo, safeToString)
        }

        return if (params.timeout > 0) {
            throw UnsupportedOperationException("verification with timeout is not supported for JS")
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

    override val verificationAcknowledger = CommonVerificationAcknowledger(stubRepo, safeToString)

    val commonCallRecorder: CommonCallRecorder = CommonCallRecorder(
        stubRepo,
        instantiator,
        signatureValueGenerator,
        mockFactory,
        { anyValueGenerator },
        safeToString,
        callRecorderFactories,
        { recorder -> callRecorderFactories.answeringState(recorder) },
        verificationAcknowledger
    )
    override val callRecorder: CallRecorder = commonCallRecorder

    override val stubber: Stubber = EveryBlockEvaluator({ callRecorder }, ::AutoHinter)
    override val verifier: Verifier = VerifyBlockEvaluator({ callRecorder }, stubRepo, ::AutoHinter)
    override val excluder: Excluder = ExcludeBlockEvaluator({ callRecorder }, stubRepo, ::AutoHinter)
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


