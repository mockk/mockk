package io.mockk

import kotlin.reflect.KClass

/**
 * Mediates mocking implementation
 */
interface MockKGateway {
    val mockFactory: MockFactory
    val staticMockFactory: StaticMockFactory
    val objectMockFactory: ObjectMockFactory
    val constructorMockFactory: ConstructorMockFactory
    val stubber: Stubber
    val verifier: Verifier
    val excluder: Excluder
    val callRecorder: CallRecorder
    val instanceFactoryRegistry: InstanceFactoryRegistry
    val clearer: Clearer
    val mockInitializer: MockInitializer
    val verificationAcknowledger: VerificationAcknowledger
    val mockTypeChecker: MockTypeChecker

    fun verifier(params: VerificationParameters): CallVerifier

    data class ClearOptions(
        val answers: Boolean,
        val recordedCalls: Boolean,
        val childMocks: Boolean,
        val verificationMarks: Boolean,
        val exclusionRules: Boolean
    )

    companion object {
        lateinit var implementation: () -> MockKGateway
    }

    /**
     * Create new mocks or spies
     */
    interface MockFactory {
        fun <T : Any> mockk(
            mockType: KClass<T>,
            name: String?,
            relaxed: Boolean,
            moreInterfaces: Array<out KClass<*>>,
            relaxUnitFun: Boolean
        ): T

        fun <T : Any> spyk(
            mockType: KClass<T>?,
            objToCopy: T?,
            name: String?,
            moreInterfaces: Array<out KClass<*>>,
            recordPrivateCalls: Boolean
        ): T

        fun temporaryMock(mockType: KClass<*>): Any

        fun isMock(value: Any): Boolean
    }


    /**
     * Binds static mocks
     */
    interface StaticMockFactory {
        fun staticMockk(cls: KClass<*>): () -> Unit

        fun clear(type: KClass<*>, options: ClearOptions)

        fun clearAll(options: ClearOptions, currentThreadOnly: Boolean)
    }

    /**
     * Binds object mocks
     */
    interface ObjectMockFactory {
        fun objectMockk(obj: Any, recordPrivateCalls: Boolean): () -> Unit

        fun clear(obj: Any, options: ClearOptions)

        fun clearAll(options: ClearOptions, currentThreadOnly: Boolean)
    }

    /**
     * Controls constructor mocking
     */
    interface ConstructorMockFactory {
        fun constructorMockk(
            cls: KClass<*>,
            recordPrivateCalls: Boolean,
            localToThread: Boolean
        ): () -> Unit

        fun <T : Any> mockPlaceholder(cls: KClass<T>, args: Array<Matcher<*>>? = null): T

        fun clear(type: KClass<*>, options: ClearOptions)

        fun clearAll(options: ClearOptions, currentThreadOnly: Boolean)
    }

    /**
     * Clears mocks
     */
    interface Clearer {
        fun clear(
            mocks: Array<out Any>,
            options: ClearOptions
        )

        fun clearAll(
            options: ClearOptions,
            currentThreadOnly: Boolean
        )
    }

    /**
     * Stub calls
     */
    interface Stubber {
        fun <T> every(
            mockBlock: (MockKMatcherScope.() -> T)?,
            coMockBlock: (suspend MockKMatcherScope.() -> T)?
        ): MockKStubScope<T, T>
    }

    /**
     * Verify calls
     */
    interface Verifier {
        fun verify(
            params: VerificationParameters,
            mockBlock: (MockKVerificationScope.() -> Unit)?,
            coMockBlock: (suspend MockKVerificationScope.() -> Unit)?
        )
    }

    /**
     * Verify calls
     */
    interface Excluder {
        fun exclude(
            params: ExclusionParameters,
            mockBlock: (MockKMatcherScope.() -> Unit)?,
            coMockBlock: (suspend MockKMatcherScope.() -> Unit)?
        )
    }

    /**
     * Parameters of verification
     */
    data class VerificationParameters(
        val ordering: Ordering,
        val min: Int,
        val max: Int,
        val inverse: Boolean,
        val timeout: Long
    )

    /**
     * Parameters of exclusion
     */
    data class ExclusionParameters(
        val current: Boolean
    )

    interface AnswerOpportunity<T> {
        fun provideAnswer(answer: Answer<T>)
    }

    /**
     * Builds a list of calls
     */
    interface CallRecorder {
        val calls: List<RecordedCall>

        fun startStubbing()

        fun startVerification(params: VerificationParameters)

        fun startExclusion(params: ExclusionParameters)

        fun round(n: Int, total: Int = 64)

        fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T

        fun call(invocation: Invocation): Any?

        fun answerOpportunity(): AnswerOpportunity<*>

        fun done()

        fun hintNextReturnType(cls: KClass<*>, n: Int)

        fun reset()

        fun estimateCallRounds(): Int

        fun nCalls(): Int

        fun wasNotCalled(list: List<Any>)

        fun discardLastCallRound()

        fun isLastCallReturnsNothing(): Boolean
    }

    /**
     * Verifier takes the list of calls and checks what invocations happened to the mocks
     */
    interface CallVerifier {
        fun verify(verificationSequence: List<RecordedCall>, params: VerificationParameters): VerificationResult

        fun captureArguments()
    }

    /**
     * Result of verification
     */
    sealed class VerificationResult {
        data class OK(val verifiedCalls: List<Invocation>) : VerificationResult()
        data class Failure(val message: String) : VerificationResult()

        val matches: Boolean
            get() = this is OK
    }


    interface InstanceFactoryRegistry {
        fun registerFactory(factory: InstanceFactory)

        fun deregisterFactory(factory: InstanceFactory)
    }

    /**
     * Factory of dummy objects
     */
    interface InstanceFactory {
        fun instantiate(cls: KClass<*>): Any?
    }

    interface MockInitializer {
        fun initAnnotatedMocks(
            targets: List<Any>,
            overrideRecordPrivateCalls: Boolean,
            relaxUnitFun: Boolean,
            relaxed: Boolean
        )
    }

    interface VerificationAcknowledger {
        fun markCallVerified(invocation: Invocation)

        fun acknowledgeVerified(mock: Any)

        fun acknowledgeVerified()

        fun checkUnnecessaryStub(mock: Any)

        fun checkUnnecessaryStub()
    }

    interface MockTypeChecker {
        fun isRegularMock(mock: Any): Boolean

        fun isSpy(mock: Any): Boolean

        fun isObjectMock(mock: Any): Boolean

        fun isStaticMock(mock: Any): Boolean

        fun isConstructorMock(mock: Any): Boolean
    }
}
