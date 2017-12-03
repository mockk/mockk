package io.mockk.impl

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
    internal val factoryRegistryIntrnl: InstanceFactoryRegistryImpl = InstanceFactoryRegistryImpl()
    override val factoryRegistry: InstanceFactoryRegistry = factoryRegistryIntrnl

    val stubRepo = StubRepository()
    internal val instantiator = Instantiator(MockKProxyMaker.INSTANCE, factoryRegistryIntrnl)
    internal val anyValueGenerator = JvmAnyValueGenerator()
    internal val signatureValueGenerator = JvmSignatureValueGenerator(Random())


    override val mockFactory: MockFactory = MockFactoryImpl(
            MockKProxyMaker.INSTANCE,
            instantiator,
            stubRepo)

    internal val unorderedVerifier = UnorderedCallVerifierImpl(stubRepo)
    internal val allVerifier = AllCallVerifierImpl(stubRepo)
    internal val orderedVerifier = OrderedCallVerifierImpl(stubRepo)
    internal val sequenceVerifier = SequenceCallVerifierImpl(stubRepo)

    override fun verifier(ordering: Ordering): CallVerifier =
            when (ordering) {
                Ordering.UNORDERED -> unorderedVerifier
                Ordering.ALL -> allVerifier
                Ordering.ORDERED -> orderedVerifier
                Ordering.SEQUENCE -> sequenceVerifier
            }

    private val callRecorderTL = object : ThreadLocal<CallRecorderImpl>() {
        override fun initialValue() = CallRecorderImpl(
                stubRepo,
                instantiator,
                signatureValueGenerator,
                mockFactory,
                anyValueGenerator)
    }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()

    override val stubber: Stubber = StubberImpl(callRecorderTL::get)
    override val verifier: Verifier = VerifierImpl(callRecorderTL::get, stubRepo, this::verifier)

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

}


