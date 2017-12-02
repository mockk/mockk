package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.*
import io.mockk.jvm.JvmAnyValueGenerator
import io.mockk.jvm.JvmSignatureValueGenerator
import io.mockk.proxy.MockKInstrumentation
import io.mockk.proxy.MockKInstrumentationLoader
import io.mockk.proxy.MockKProxyMaker
import java.util.*
import kotlin.reflect.KClass


class MockKGatewayImpl : MockKGateway {
    internal val stubs = InternalPlatform.weakMap<Any, Stub>()

    override val mockFactory: MockFactory = MockFactoryImpl(this)
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
        private val log = logger<MockKGatewayImpl>()

        init {
            loggerFactory = slf4jOrJulLogging()

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

    fun stubFor(mock: Any): Stub = stubs[mock]
            ?: throw MockKException("can't find stub for $mock")

    interface Stub {
        val name: String

        val type: KClass<*>

        fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>)

        fun answer(invocation: Invocation): Any?

        fun childMockK(call: MatchedCall): Any?

        fun recordCall(invocation: Invocation)

        fun allRecordedCalls(): List<Invocation>

        fun clear(answers: Boolean, calls: Boolean, childMocks: Boolean)

        fun handleInvocation(self: Any,
                             method: MethodDescription,
                             originalCall: () -> Any?,
                             args: Array<out Any?>): Any?

        fun toStr(): String
    }

    interface Instantiator {
        fun <T : Any> instantiate(cls: KClass<T>): T

        fun <T : Any> proxy(cls: KClass<T>,
                            useDefaultConstructor: Boolean,
                            instantiateOnFailure: Boolean,
                            moreInterfaces: Array<out KClass<*>>, stub: Stub): Any

        fun isPassedByValue(cls: KClass<*>): Boolean

        fun staticMockk(cls: KClass<*>, stub: Stub)

        fun staticUnMockk(cls: KClass<*>)
    }

}

