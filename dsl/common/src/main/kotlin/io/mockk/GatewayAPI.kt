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
    val callRecorder: CallRecorder
    val instanceFactoryRegistry: InstanceFactoryRegistry
    val clearer: Clearer
    val mockInitializer: MockInitializer

    fun verifier(params: VerificationParameters): CallVerifier

    data class ClearOptions(
        val answers: Boolean,
        val recordedCalls: Boolean,
        val childMocks: Boolean
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

        fun clearAll(options: ClearOptions)
    }

    /**
     * Binds object mocks
     */
    interface ObjectMockFactory {
        fun objectMockk(obj: Any, recordPrivateCalls: Boolean): () -> Unit

        fun clear(obj: Any, options: ClearOptions)

        fun clearAll(options: ClearOptions)
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

        fun <T : Any> mockPlaceholder(cls: KClass<T>): T

        fun clear(type: KClass<*>, options: ClearOptions)

        fun clearAll(options: ClearOptions)
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
            options: ClearOptions
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
     * Builds a list of calls
     */
    interface CallRecorder {
        val calls: List<RecordedCall>

        fun startStubbing()

        fun startVerification(params: VerificationParameters)

        fun round(n: Int, total: Int = 64)

        fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T

        fun call(invocation: Invocation): Any?

        fun answer(answer: Answer<*>)

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
    data class VerificationResult(val matches: Boolean, val message: String? = null)


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
}
