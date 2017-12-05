package io.mockk

import kotlin.reflect.KClass

/**
 * Mediates mocking implementation
 */
interface MockKGateway {
    val mockFactory: MockFactory
    val stubbingRecorder: Stubber
    val verifyingRecorder: Verifier
    val callRecorder: CallRecorder
    val instanceFactoryRegistry: InstanceFactoryRegistry

    fun verifier(ordering: Ordering): CallVerifier

    companion object {
        lateinit var implementation: () -> MockKGateway
    }

    /**
     * Create new mocks or spies
     */
    interface MockFactory {
        fun <T : Any> mockk(cls: KClass<T>,
                            name: String?,
                            moreInterfaces: Array<out KClass<*>>): T

        fun <T : Any> spyk(cls: KClass<T>?,
                           objToCopy: T?,
                           name: String?,
                           moreInterfaces: Array<out KClass<*>>): T

        fun staticMockk(cls: KClass<*>)
        fun staticUnMockk(cls: KClass<*>)

        fun childMock(cls: KClass<*>): Any

        fun clear(mocks: Array<out Any>,
                  answers: Boolean,
                  recordedCalls: Boolean,
                  childMocks: Boolean)
    }


    /**
     * Stub calls
     */
    interface Stubber {
        fun <T> every(mockBlock: (MockKMatcherScope.() -> T)?,
                      coMockBlock: (suspend MockKMatcherScope.() -> T)?): MockKStubScope<T>
    }

    /**
     * Verify calls
     */
    interface Verifier {
        fun verify(params: VerificationParameters,
                   mockBlock: (MockKVerificationScope.() -> Unit)?,
                   coMockBlock: (suspend MockKVerificationScope.() -> Unit)?)
    }

    /**
     * Parameters of verification
     */
    data class VerificationParameters(val ordering: Ordering,
                                      val min: Int,
                                      val max: Int,
                                      val inverse: Boolean)


    /**
     * Builds a list of calls
     */
    interface CallRecorder {
        val calls: List<MatchedCall>

        fun startStubbing()

        fun startVerification(params: VerificationParameters)

        fun catchArgs(round: Int, n: Int = 64)

        fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T

        fun call(invocation: Invocation): Any?

        fun answer(answer: Answer<*>)

        fun done()

        fun hintNextReturnType(cls: KClass<*>, n: Int)

        fun reset()

        fun estimateCallRounds(): Int

        fun nCalls(): Int

        fun wasNotCalled(list: List<Any>)
    }

    /**
     * Verifier takes the list of calls and checks what invocations happened to the mocks
     */
    interface CallVerifier {
        fun verify(calls: List<MatchedCall>, min: Int, max: Int): VerificationResult
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
}


interface Ref {
    val value: Any
}
