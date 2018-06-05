package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKCancellation
import io.mockk.MockKException
import io.mockk.MockKGateway.ConstructorMockFactory
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.CommonClearer
import io.mockk.impl.stub.ConstructorStub
import io.mockk.impl.stub.SpyKStub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.proxy.Cancelable
import io.mockk.proxy.MockKConstructorProxyMaker
import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.MockKProxyMaker
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Callable
import kotlin.concurrent.getOrSet
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

class JvmConstructorMockFactory(
    val constructorProxyMaker: MockKConstructorProxyMaker,
    val clearer: CommonClearer,
    val mockFactory: AbstractMockFactory,
    val objectProxyMaker: MockKProxyMaker,
    val gatewayAccess: StubGatewayAccess
) : ConstructorMockFactory {
    val log = gatewayAccess.safeLog(Logger<JvmConstructorMockFactory>())

    inner class ConstructorMock(
        val cls: KClass<*>,
        val recordPrivateCalls: Boolean
    ) {
        val cancellations = mutableListOf<MockKCancellation>()

        val name = "mockkConstructor<${cls.simpleName}>()"

        init {
            log.trace { "Creating constructor representation mock for ${cls.toStr()}" }
        }

        val representativeStub = SpyKStub(cls, name, gatewayAccess, true)
        val representativeMock = mockFactory.newProxy(cls, arrayOf(), representativeStub)

        init {
            with(representativeStub){
                hashCodeStr = InternalPlatform.hkd(representativeMock)
                disposeRoutine = this@ConstructorMock::dispose

                gatewayAccess.stubRepository.add(representativeMock, this)
            }
        }

        fun dispose() {
            cancellations.forEach { it() }
            cancellations.clear()
        }
    }


    inner class ConstructorInvocationHandler(val cls: KClass<*>) : MockKInvocationHandler {
        var global = Stack<ConstructorMock>()
        var local = ThreadLocal<Stack<ConstructorMock>>()
        var nLocals = 0

        var cancelable: Cancelable<Class<*>>? = null

        val constructorMock: ConstructorMock?
            get() = local.get()?.lastOrNull() ?: global.lastOrNull()

        override fun invocation(
            self: Any,
            method: Method?,
            originalCall: Callable<*>?,
            args: Array<Any?>
        ): Any? {
            val mock = constructorMock
                    ?: throw MockKException("Bad constructor mock handler for ${self::class}")

            log.trace { "Connecting just created object to constructor representation mock for ${cls.toStr()}" }

            val stub = ConstructorStub(
                self,
                mock.representativeMock,
                mock.representativeStub,
                mock.recordPrivateCalls
            )

            val cancellation = objectProxyMaker.proxy(
                cls.java,
                arrayOf(),
                JvmMockFactoryHelper.mockHandler(
                    stub
                ),
                false,
                self
            )

            gatewayAccess.stubRepository.add(self, stub)

            mock.cancellations.add(cancellation::cancel)

            return Unit
        }

        fun push(
            threadLocal: Boolean,
            recordPrivateCalls: Boolean
        ): () -> Unit {
            if (cancelable == null) {
                cancelable = constructorProxyMaker.constructorProxy(cls.java, this)
            }

            val repr = ConstructorMock(cls, recordPrivateCalls)

            if (threadLocal) {
                local.getOrSet { nLocals++; Stack() }.push(repr)
            } else {
                global.push(repr)
            }

            return {
                doCancel(threadLocal, repr)
            }

        }

        private fun doCancel(
            threadLocal: Boolean,
            repr: ConstructorMock
        ) {
            if (threadLocal) {
                val stack = local.get()
                if (stack != null) {
                    stack.remove(repr)
                    if (stack.isEmpty()) {
                        local.remove()
                        nLocals--
                    }
                }
            } else {
                global.remove(repr)
            }

            repr.dispose()

            if (nLocals == 0 && global.isEmpty()) {
                cancelable?.cancel()
                cancelable = null
                handlers.remove(cls)
            }
        }
    }


    val handlers = WeakHashMap<KClass<*>, ConstructorInvocationHandler>()

    override fun constructorMockk(
        cls: KClass<*>,
        recordPrivateCalls: Boolean,
        localToThread: Boolean
    ): () -> Unit {
        return synchronized(handlers) {
            val handler = handlers.getOrPut(cls, {
                ConstructorInvocationHandler(cls)
            })

            handler.push(localToThread, recordPrivateCalls)
        }
    }


    override fun <T : Any> mockPlaceholder(cls: KClass<T>) = cls.cast(
        getMock(cls)
                ?: throw MockKException("to use anyConstructed<T>() first build mockkConstructor<T>() and 'use' it")
    )

    override fun clear(type: KClass<*>, answers: Boolean, recordedCalls: Boolean, childMocks: Boolean) {
        getMock(type)?.let {
            clearer.clear(arrayOf(it), answers, recordedCalls, childMocks)
        }
    }

    private fun <T : Any> getMock(cls: KClass<T>): Any? {
        return synchronized(handlers) {
            handlers[cls]?.constructorMock?.representativeMock
        }
    }
}
