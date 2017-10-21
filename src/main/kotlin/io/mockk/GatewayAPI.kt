package io.mockk

import io.mockk.impl.MockKGatewayImpl

/**
 * Mediates mocking implementation
 */
interface MockKGateway {
    val callRecorder: CallRecorder
    val instantiator: Instantiator
    fun verifier(ordering: Ordering): Verifier


    fun <T> mockk(cls: Class<T>): T

    fun <T> spyk(cls: Class<T>, objToCopy: T?): T

    fun <T> every(mockBlock: suspend MockKScope.() -> T): MockKStubScope<T>

    fun <T> verify(ordering: Ordering,
                   inverse: Boolean,
                   atLeast: Int,
                   atMost: Int,
                   exactly: Int,
                   mockBlock: suspend MockKScope.() -> T)

    companion object {
        internal val defaultImpl: MockKGateway = MockKGatewayImpl()
        var LOCATOR: () -> MockKGateway = { defaultImpl }

        val NO_ARG_TYPE_NAME = MockK::class.java.name + "NoArgParam"
    }
}

/**
 * Backs DSL and build a list of calls
 */
interface CallRecorder {
    fun startStubbing()

    fun startVerification()

    fun catchArgs(round: Int, n: Int)

    fun <T> matcher(matcher: Matcher<*>, cls: Class<T>): T

    fun call(invocation: Invocation): Any?

    fun answer(answer: Answer<*>)

    fun verify(ordering: Ordering, inverse: Boolean, min: Int, max: Int)

    fun childType(cls: Class<*>, n: Int)
}

/**
 * Verifier takes the list of calls and checks what invocations happened to the mocks
 */
interface Verifier {
    fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult
}

/**
 * Result of verfication
 */
data class VerificationResult(val matches: Boolean, val matcher: InvocationMatcher? = null)

/**
 * Instantiates empty object for provided class
 */
interface Instantiator {
    fun <T> instantiate(cls: Class<T>): T

    fun anyValue(cls: Class<*>, orInstantiateVia: () -> Any? = { instantiate(cls) }): Any?

    fun <T> proxy(cls: Class<T>, useDefaultConstructor: Boolean): Any

    fun <T> signatureValue(cls: Class<T>): T

    fun isPassedByValue(cls: Class<*>): Boolean
}
