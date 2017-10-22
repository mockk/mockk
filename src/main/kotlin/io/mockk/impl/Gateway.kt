package io.mockk.impl

import io.mockk.*
import io.mockk.external.logger
import javassist.util.proxy.ProxyObject
import kotlinx.coroutines.experimental.runBlocking


internal class MockKGatewayImpl : MockKGateway {
    private val log = logger<MockKGatewayImpl>()

    private val callRecorderTL = ThreadLocal.withInitial { CallRecorderImpl(this) }
    private val instantiatorTL = ThreadLocal.withInitial { InstantiatorImpl(this) }
    private val unorderedVerifierTL = ThreadLocal.withInitial { UnorderedVerifierImpl(this) }
    private val orderedVerifierTL = ThreadLocal.withInitial { OrderedVerifierImpl(this) }
    private val sequenceVerifierTL = ThreadLocal.withInitial { SequenceVerifierImpl(this) }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()

    override val instantiator: Instantiator
        get() = instantiatorTL.get()

    override fun verifier(ordering: Ordering) : Verifier =
            when (ordering) {
                Ordering.UNORDERED -> unorderedVerifierTL.get()
                Ordering.ORDERED -> orderedVerifierTL.get()
                Ordering.SEQUENCE -> sequenceVerifierTL.get()
            }


    override fun <T> mockk(cls: Class<T>): T {
        log.debug { "Creating mockk for $cls" }
        val obj = instantiator.proxy(cls, false)
        (obj as ProxyObject).handler = MockKInstanceProxyHandler(cls, obj)
        return cls.cast(obj)
    }

    override fun <T> spyk(cls: Class<T>, objToCopy: T?): T {
        log.debug { "Creating spyk for $cls" }
        val obj = instantiator.proxy(cls, objToCopy == null)
        if (objToCopy != null) {
            copyFields(obj, objToCopy as Any)
        }
        (obj as ProxyObject).handler = SpyKInstanceProxyHandler(cls, obj)
        return cls.cast(obj)
    }

    private fun copyFields(obj: Any, objToCopy: Any) {
        for (field in objToCopy.javaClass.declaredFields) {
            field.isAccessible = true
            field.set(obj, field.get(objToCopy))
            log.trace { "Copied field $field" }
        }
    }

    override fun <T> every(mockBlock: suspend MockKScope.() -> T): MockKStubScope<T> {
        callRecorder.startStubbing()
        val lambda = slot<Function<*>>()
        val scope = MockKScope(this, lambda)
        runBlocking {
            val n = N_CALL_ROUNDS
            repeat(n) {
                callRecorder.catchArgs(it, n)
                scope.mockBlock()
            }
            callRecorder.catchArgs(n, n)
        }
        return MockKStubScope(this, lambda)
    }

    override fun <T> verify(ordering: Ordering, inverse: Boolean, atLeast: Int, atMost: Int, exactly: Int, mockBlock: suspend MockKScope.() -> T) {
        if (ordering != Ordering.UNORDERED) {
            if (atLeast != 1 || atMost != Int.MAX_VALUE || exactly != -1) {
                throw MockKException("atLeast, atMost, exactly is only allowed in unordered verify block")
            }
        }

        val gw = MockKGateway.LOCATOR()
        val callRecorder = gw.callRecorder
        callRecorder.startVerification()

        val lambda = slot<Function<*>>()
        val scope = MockKScope(gw, lambda)

        runBlocking {
            val n = N_CALL_ROUNDS
            repeat(n) {
                callRecorder.catchArgs(it, n)
                scope.mockBlock()
            }
            callRecorder.catchArgs(n, n)
        }
        callRecorder.verify(ordering, inverse,
                if (exactly != -1) exactly else atLeast,
                if (exactly != -1) exactly else atMost)
    }

    companion object {
        val N_CALL_ROUNDS = 64

        val log = logger<MockKGatewayImpl>()

        init {
            log.debug { "Starting MockK implementation. Java version = ${System.getProperty("java.version")}" }
        }
    }
}
