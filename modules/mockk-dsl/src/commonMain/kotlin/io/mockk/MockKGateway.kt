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
        val exclusionRules: Boolean,
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
            relaxUnitFun: Boolean,
        ): T

        fun <T : Any> spyk(
            mockType: KClass<T>?,
            objToCopy: T?,
            name: String?,
            moreInterfaces: Array<out KClass<*>>,
            recordPrivateCalls: Boolean,
        ): T

        fun temporaryMock(mockType: KClass<*>): Any

        fun isMock(value: Any): Boolean
    }

    /**
     * Binds static mocks
     */
    interface StaticMockFactory {
        fun staticMockk(cls: KClass<*>): () -> Unit

        fun clear(
            type: KClass<*>,
            options: ClearOptions,
        )

        fun clearAll(
            options: ClearOptions,
            currentThreadOnly: Boolean,
        )
    }

    /**
     * Binds object mocks
     */
    interface ObjectMockFactory {
        fun objectMockk(
            obj: Any,
            recordPrivateCalls: Boolean,
        ): () -> Unit

        fun clear(
            obj: Any,
            options: ClearOptions,
        )

        fun clearAll(
            options: ClearOptions,
            currentThreadOnly: Boolean,
        )
    }

    /**
     * Controls constructor mocking
     */
    interface ConstructorMockFactory {
        fun constructorMockk(
            cls: KClass<*>,
            recordPrivateCalls: Boolean,
            localToThread: Boolean,
        ): () -> Unit

        fun <T : Any> mockPlaceholder(
            cls: KClass<T>,
            args: Array<Matcher<*>>? = null,
        ): T

        /**
         * Returns all representative mocks that currently stand for constructed instances
         * of [cls]: the aggregate representative used by `anyConstructed<T>()` plus one
         * representative per distinct constructor invocation pattern. Returns an empty
         * list when there is no active `mockkConstructor<T>()` session for [cls].
         */
        fun representativeMocks(cls: KClass<*>): List<Any> = emptyList()

        fun clear(
            type: KClass<*>,
            options: ClearOptions,
        )

        fun clearAll(
            options: ClearOptions,
            currentThreadOnly: Boolean,
        )
    }

    /**
     * Clears mocks
     */
    interface Clearer {
        fun clear(
            mocks: Array<out Any>,
            options: ClearOptions,
        )

        fun clearAll(
            options: ClearOptions,
            currentThreadOnly: Boolean,
        )

        /**
         * Clears every requested kind of mock in a single pass over the stub repository.
         *
         * [clearAll] and the `clearAll` of [ObjectMockFactory], [StaticMockFactory] and
         * [ConstructorMockFactory] each walk every live stub, so clearing all four kinds through
         * them costs four passes to clear each mock once.
         */
        fun clearAll(
            options: ClearOptions,
            currentThreadOnly: Boolean,
            regularMocks: Boolean,
            objectMocks: Boolean,
            staticMocks: Boolean,
            constructorMocks: Boolean,
        )

        fun clearAllStubsFromMemory(
            currentThreadOnly: Boolean = false,
            excludeMocks: List<Any> = emptyList(),
        )
    }

    /**
     * Stub calls
     */
    interface Stubber {
        fun <T> every(
            mockBlock: (MockKMatcherScope.() -> T)?,
            coMockBlock: (suspend MockKMatcherScope.() -> T)?,
        ): MockKStubScope<T, T> = every(mockBlock, coMockBlock, lenient = false)

        /**
         * Same as [every], with [lenient] marking the freshly created stubs as exempt from
         * the unnecessary stubbing check ([VerificationAcknowledger.checkUnnecessaryStub]).
         */
        fun <T> every(
            mockBlock: (MockKMatcherScope.() -> T)?,
            coMockBlock: (suspend MockKMatcherScope.() -> T)?,
            lenient: Boolean,
        ): MockKStubScope<T, T>
    }

    /**
     * Verify calls
     */
    interface Verifier {
        fun verify(
            params: VerificationParameters,
            mockBlock: (MockKVerificationScope.() -> Unit)?,
            coMockBlock: (suspend MockKVerificationScope.() -> Unit)?,
        )
    }

    /**
     * Verify calls
     */
    interface Excluder {
        fun exclude(
            params: ExclusionParameters,
            mockBlock: (MockKMatcherScope.() -> Unit)?,
            coMockBlock: (suspend MockKMatcherScope.() -> Unit)?,
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
        val timeout: Long,
    )

    /**
     * Parameters of exclusion
     */
    data class ExclusionParameters(
        val current: Boolean,
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

        fun round(
            n: Int,
            total: Int = 64,
        )

        fun <T : Any> matcher(
            matcher: Matcher<*>,
            cls: KClass<T>,
        ): T

        fun call(invocation: Invocation): Any?

        fun answerOpportunity(): AnswerOpportunity<*>

        fun done()

        fun hintNextReturnType(
            cls: KClass<*>,
            n: Int,
        )

        fun reset()

        /**
         * Takes the ongoing recording/verification session out of the recorder while the
         * coroutine scaffolding that will run the recorded block initializes itself.
         *
         * Entering the coroutine for a `coEvery`/`coVerify` block consults the global
         * coroutine context (for example the dispatcher lookups done by `runBlocking`).
         * With a static mock installed on a global object such as `Dispatchers`, those
         * lookups are intercepted calls that would otherwise be recorded as part of the
         * block itself (issue #1247). While the session is paused they are answered
         * outside of it, exactly like calls made before the block started.
         *
         * The session is restored by [resumeSession], called once the scaffolding is done
         * and the recorded block is about to run. If the block fails, [cancelRecordingSession]
         * discards the paused session together with the rest of the recording sequence.
         */
        fun pauseSession()

        /**
         * Restores the session paused by [pauseSession] once the coroutine scaffolding has
         * finished and the recorded block itself is about to start.
         */
        fun resumeSession()

        /**
         * Cancels the current recording session. A suspended parent session is handed back
         * to its owner, which is still executing its own recorded block and continues once
         * the failed session is gone (a failing nested `coVerify` interleaved into a
         * `coEvery`/`coVerify` block window, issue #1398). A session paused by
         * [pauseSession] belongs to the failing sequence itself and is discarded together
         * with any suspended nested ones, returning the recorder to its idle state. Unlike
         * [reset], which resumes a suspended parent session when it finalizes a nested one,
         * this is for error paths where the current session cannot be finalized.
         */
        fun cancelRecordingSession()

        fun estimateCallRounds(): Int

        fun nCalls(): Int

        fun wasNotCalled(list: List<Any>)

        fun discardLastCallRound()

        fun isLastCallReturnsNothing(): Boolean

        /**
         * Registers a constructor placeholder mock (the representative mock returned by
         * `anyConstructed<T>()`) requested during the current recording session.
         *
         * During verification the placeholder itself then acts as an aggregate verification
         * target: `verify(exactly = 0) { anyConstructed<T>() }` checks that no calls were
         * recorded on any constructed instance of `T`.
         */
        fun constructorPlaceholder(mock: Any) {}
    }

    /**
     * Verifier takes the list of calls and checks what invocations happened to the mocks
     */
    interface CallVerifier {
        fun verify(
            verificationSequence: List<RecordedCall>,
            params: VerificationParameters,
        ): VerificationResult

        fun captureArguments()
    }

    /**
     * Result of verification
     */
    sealed class VerificationResult {
        data class OK(
            val verifiedCalls: List<Invocation>,
        ) : VerificationResult()

        data class Failure(
            val message: String,
        ) : VerificationResult()

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
            relaxed: Boolean,
        ) {
            initAnnotatedMocks(targets, overrideRecordPrivateCalls, relaxUnitFun, relaxed, false)
        }

        fun initAnnotatedMocks(
            targets: List<Any>,
            overrideRecordPrivateCalls: Boolean,
            relaxUnitFun: Boolean,
            relaxed: Boolean,
            useDependencyOrder: Boolean,
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
