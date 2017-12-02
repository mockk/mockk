package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.*
import io.mockk.external.adaptor
import io.mockk.external.logger
import io.mockk.proxy.MockKInstrumentation
import io.mockk.proxy.MockKInstrumentationLoader
import io.mockk.proxy.MockKProxyMaker
import java.util.*
import java.util.Collections.synchronizedMap
import kotlin.reflect.KClass

interface Instantiator {
    fun <T : Any> instantiate(cls: KClass<T>): T

    fun anyValue(cls: KClass<*>, orInstantiateVia: () -> Any? = { instantiate(cls) }): Any?

    fun <T : Any> proxy(cls: KClass<T>,
                        useDefaultConstructor: Boolean,
                        instantiateOnFailure: Boolean,
                        moreInterfaces: Array<out KClass<*>>, stub: Stub): Any

    fun <T : Any> signatureValue(cls: KClass<T>): T

    fun isPassedByValue(cls: KClass<*>): Boolean

    fun staticMockk(cls: KClass<*>, stub: Stub)

    fun staticUnMockk(cls: KClass<*>)
}


class MockKGatewayImpl : MockKGateway {
    internal val stubs = synchronizedMap(IdentityHashMap<Any, Stub>())

    override val mockFactory: MockFactory = MockFactoryImpl(this)
    override val stubber: Stubber = StubberImpl(this)
    override val verifier: Verifier = VerifierImpl(this)
    override val factoryRegistry: InstanceFactoryRegistryImpl = InstanceFactoryRegistryImpl(this)

    internal val instantiator = InstantiatorImpl(this)

    internal val unorderedVerifier = UnorderedCallVerifierImpl(this)
    internal val allVerifier = AllCallVerifierImpl(this)
    internal val orderedVerifier = OrderedCallVerifierImpl(this)
    internal val sequenceVerifier = SequenceCallVerifierImpl(this)

    override fun verifier(ordering: Ordering): CallVerifier =
            when (ordering) {
                Ordering.UNORDERED -> unorderedVerifier
                Ordering.ALL -> allVerifier
                Ordering.ORDERED -> orderedVerifier
                Ordering.SEQUENCE -> sequenceVerifier
            }

    private val callRecorderTL = object : ThreadLocal<CallRecorderImpl>() {
        override fun initialValue() = CallRecorderImpl(this@MockKGatewayImpl)
    }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()


    companion object {
        private val log = logger<MockKGatewayImpl>()

        init {
            log.trace {
                "Starting Java MockK implementation. " +
                        "Java version = ${System.getProperty("java.version")}. "
            }

            MockKProxyMaker.log = logger<MockKProxyMaker>().adaptor()
            MockKInstrumentationLoader.log = logger<MockKInstrumentationLoader>().adaptor()
            MockKInstrumentation.log = logger<MockKInstrumentation>().adaptor()

            MockKInstrumentation.init()
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
}

