package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.*
import io.mockk.agent.MockKAgentLogger
import io.mockk.external.Logger
import io.mockk.external.adaptor
import io.mockk.external.logger
import io.mockk.proxy.MockKInstrumentation
import io.mockk.proxy.MockKProxyMaker
import kotlinx.coroutines.experimental.runBlocking
import java.lang.Exception
import java.util.*
import java.util.Collections.synchronizedMap


class MockKGatewayImpl : MockKGateway {
    internal val stubs = synchronizedMap(IdentityHashMap<Any, Stub>())

    private val mockFactoryTL = threadLocalOf { MockFactoryImpl(this) }
    private val stubberTL = threadLocalOf { StubberImpl(this) }
    private val verifierTL = threadLocalOf { VerifierImpl(this) }
    private val callRecorderTL = threadLocalOf { CallRecorderImpl(this) }
    private val instantiatorTL = threadLocalOf { InstantiatorImpl(this) }
    private val unorderedVerifierTL = threadLocalOf { UnorderedCallVerifierImpl(this) }
    private val orderedVerifierTL = threadLocalOf { OrderedCallVerifierImpl(this) }
    private val sequenceVerifierTL = threadLocalOf { SequenceCallVerifierImpl(this) }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()

    override val instantiator: Instantiator
        get() = instantiatorTL.get()

    override val mockFactory: MockFactory
        get() = mockFactoryTL.get()

    override val stubber: Stubber
        get() = stubberTL.get()

    override val verifier: Verifier
        get() = verifierTL.get()

    override fun verifier(ordering: Ordering): CallVerifier =
            when (ordering) {
                Ordering.UNORDERED -> unorderedVerifierTL.get()
                Ordering.ORDERED -> orderedVerifierTL.get()
                Ordering.SEQUENCE -> sequenceVerifierTL.get()
            }


    companion object {

        private val log = logger<MockKGatewayImpl>()

        init {
            log.trace {
                "Starting Java MockK implementation. " +
                        "Java version = ${System.getProperty("java.version")}. "
            }
            MockKProxyMaker.log = logger<MockKProxyMaker>().adaptor()
            MockKInstrumentation.log = logger<MockKInstrumentation>().adaptor()
        }

        val defaultImplementation = MockKGatewayImpl()
        val defaultImplementationBuilder = { defaultImplementation }

        inline fun <T> useImpl(block: () -> T): T {
            MockKGateway.implementation = defaultImplementationBuilder
            return block()
        }
    }

    override fun stubFor(mock: Any): Stub = stubs[mock]
            ?: throw MockKException("can't find stub for $mock")

    override fun <T> runCoroutine(block: suspend () -> T): T = runBlocking { block() }
}

