package io.mockk.impl

import io.mockk.InternalPlatform
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.jvm.JvmAnyValueGenerator
import io.mockk.jvm.JvmSignatureValueGenerator
import io.mockk.proxy.MockKInstrumentation
import io.mockk.proxy.MockKInstrumentationLoader
import io.mockk.proxy.MockKProxyMaker
import io.mockk.impl.JvmLogging.adaptor
import java.util.*

class MockKGatewayImpl : MockKGateway {
    internal val stubs = InternalPlatform.weakMap<Any, Stub>()

    override val mockFactory: MockFactory = MockFactoryImpl(this, MockKProxyMaker.INSTANCE)
    override val stubber: Stubber = StubberImpl(this)
    override val verifier: Verifier = VerifierImpl(this)
    internal val factoryRegistryIntrnl: InstanceFactoryRegistryImpl = InstanceFactoryRegistryImpl()
    override val factoryRegistry: InstanceFactoryRegistry = factoryRegistryIntrnl

    internal val instantiator = InstantiatorImpl(this)
    internal val anyValueGenerator = JvmAnyValueGenerator()
    internal val signatureValueGenerator = JvmSignatureValueGenerator(Random())

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
        private var log: Logger

        init {
            Logger.loggerFactory = JvmLogging.slf4jOrJulLogging()

            log = Logger<MockKGatewayImpl>()

            log.trace {
                "Starting Java MockK implementation. " +
                        "Java version = ${System.getProperty("java.version")}. "
            }

            MockKProxyMaker.log = Logger<MockKProxyMaker>().adaptor()
            MockKInstrumentationLoader.log = Logger<MockKInstrumentationLoader>().adaptor()
            MockKInstrumentation.log = Logger<MockKInstrumentation>().adaptor()

            MockKInstrumentation.init()
        }

        val defaultImplementation = MockKGatewayImpl()
        val defaultImplementationBuilder = { defaultImplementation }

        inline fun <T> useImpl(block: () -> T): T {
            MockKGateway.implementation = defaultImplementationBuilder
            return block()
        }
    }

    fun stubFor(mock: Any): Stub = stubs[mock]
            ?: throw MockKException("can't find stub for $mock")

}


