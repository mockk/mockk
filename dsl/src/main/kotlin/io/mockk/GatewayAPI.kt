package io.mockk

import kotlin.reflect.KClass

/**
 * Mediates mocking implementation
 */
interface MockKGateway {
    val mockFactory: MockFactory
    val stubber: Stubber
    val verifier: Verifier
    val callRecorder: CallRecorder
    val factoryRegistry: InstanceFactoryRegistry

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

        fun clear(mocks: Array<out Any>,
                  answers: Boolean,
                  recordedCalls: Boolean,
                  childMocks: Boolean)

        fun staticMockk(cls: KClass<*>)
        fun staticUnMockk(cls: KClass<*>)
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
        fun verify(ordering: Ordering,
                   inverse: Boolean,
                   atLeast: Int,
                   atMost: Int,
                   exactly: Int,
                   mockBlock: (MockKVerificationScope.() -> Unit)?,
                   coMockBlock: (suspend MockKVerificationScope.() -> Unit)?)

        fun checkWasNotCalled(mocks: List<Any>)
    }

    /**
     * Builds a list of calls
     */
    interface CallRecorder {
        val calls: List<MatchedCall>

        fun startStubbing()

        fun startVerification()

        fun catchArgs(round: Int, n: Int = 64)

        fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T

        fun call(invocation: Invocation): Any?

        fun answer(answer: Answer<*>)

        fun doneVerification()

        fun hintNextReturnType(cls: KClass<*>, n: Int)

        fun cancel()

        fun estimateCallRounds(): Int

        fun nCalls(): Int
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

/**
 * Platform related functions
 */
expect object InternalPlatform {
    fun identityHashCode(obj: Any): Int

    fun ref(obj: Any): Ref

    fun <T> runCoroutine(block: suspend () -> T): T

    fun Any?.toStr(): String

    fun deepEquals(obj1: Any?, obj2: Any?): Boolean

    fun <K, V> MutableMap<K, V>.customComputeIfAbsent(key: K, valueFunc: (K) -> V): V

    fun <T> synchronizedMutableList(): MutableList<T>

    fun <K, V> weakMap(): MutableMap<K, V>
}