package io.mockk.impl.instantiation

import io.mockk.EqMatcher
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.Matcher
import io.mockk.MockKCancellation
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.ConstructorMockFactory
import io.mockk.NullCheckMatcher
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.*
import io.mockk.proxy.Cancelable
import io.mockk.proxy.MockKConstructorProxyMaker
import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.MockKProxyMaker
import java.lang.reflect.Method
import java.util.Stack
import java.util.WeakHashMap
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
    val log = gatewayAccess.safeToString(Logger<JvmConstructorMockFactory>())

    inner class ConstructorMock(
        val cls: KClass<*>,
        val recordPrivateCalls: Boolean,
        argsStr: String = ""
    ) {
        val cancellations = mutableListOf<MockKCancellation>()

        val name = "mockkConstructor<${cls.simpleName}>($argsStr)"

        init {
            log.trace { "Creating constructor representation mock for ${cls.toStr()}" }
        }

        val representativeStub = SpyKStub(
            cls,
            name,
            gatewayAccess,
            true,
            MockType.CONSTRUCTOR
        )
        val representativeMock = mockFactory.newProxy(cls, arrayOf(), representativeStub)

        init {
            with(representativeStub) {
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

    inner class ConstructorMockVariant(
        val cls: KClass<*>,
        private val recordPrivateCalls: Boolean
    ) {
        private val handlers = mutableMapOf<List<Matcher<*>>, ConstructorMock>()

        private var allHandler: ConstructorMock? = null

        fun getMock(args: Array<Any?>): ConstructorMock? {
            synchronized(handlers) {
                return handlers.entries.firstOrNull {
                    matchArgs(args, it.key)
                }?.value
                    ?: allHandler
                    ?: getConstructorMock(args.map { eqOrNull(it) }.toTypedArray())
            }
        }

        private fun eqOrNull(it: Any?): Matcher<*> {
            return if (it == null) {
                NullCheckMatcher<Any>(false)
            } else {
                EqMatcher(it)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun matchArgs(args: Array<Any?>, matchers: List<Matcher<*>>): Boolean {
            if (matchers.size != args.size) {
                return false
            }

            repeat(matchers.size) {
                val arg = args[it]
                val matcher = matchers[it] as Matcher<Any>
                if (!matcher.match(arg)) {
                    return false
                }
            }

            return true
        }

        fun getRepresentative(args: Array<Matcher<*>>?) = getConstructorMock(args)?.representativeMock

        private fun getConstructorMock(args: Array<Matcher<*>>?): ConstructorMock? {
            return synchronized(handlers) {
                if (args == null) {
                    if (allHandler == null) {
                        allHandler = ConstructorMock(cls, recordPrivateCalls)
                    }
                    allHandler
                } else {
                    handlers.getOrPut(args.toList()) {
                        ConstructorMock(cls, recordPrivateCalls, args.joinToString(", ") { it.toStr() })
                    }
                }
            }
        }

        private fun allHandlers() = (handlers.values + listOfNotNull(allHandler))

        fun clear(options: MockKGateway.ClearOptions) {
            val mocks = synchronized(handlers) {
                allHandlers().map { it.representativeMock }
            }.toTypedArray()

            clearer.clear(mocks, options)
        }

        fun dispose() {
            synchronized(handlers) {
                allHandlers().forEach { it.dispose() }

                allHandler = null
                handlers.clear()
            }
        }

        override fun toString() = "ConstructorMockVariant(${cls.toStr()})"
    }


    inner class ConstructorInvocationHandler(val cls: KClass<*>) : MockKInvocationHandler {
        private var global = Stack<ConstructorMockVariant>()
        private var local = ThreadLocal<Stack<ConstructorMockVariant>>()
        private var nLocals = 0

        private var cancelable: Cancelable<Class<*>>? = null

        val constructorMockVariant: ConstructorMockVariant?
            get() = local.get()?.lastOrNull() ?: global.lastOrNull()

        override fun invocation(
            self: Any,
            method: Method?,
            originalCall: Callable<*>?,
            args: Array<Any?>
        ): Any {
            val mock = constructorMockVariant?.getMock(args)
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

            val repr = ConstructorMockVariant(cls, recordPrivateCalls)

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
            repr: ConstructorMockVariant
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
            val handler = handlers.getOrPut(cls) {
                ConstructorInvocationHandler(cls)
            }

            handler.push(localToThread, recordPrivateCalls)
        }
    }

    override fun <T : Any> mockPlaceholder(cls: KClass<T>, args: Array<Matcher<*>>?): T = cls.cast(
        getMockVariant(cls)?.getRepresentative(args)
            ?: throw MockKException("to use anyConstructed<T>() or constructedWith<T>(...) first build mockkConstructor<T>() and 'use' it")
    )


    override fun clear(type: KClass<*>, options: MockKGateway.ClearOptions) {
        getMockVariant(type)?.clear(options)
    }

    override fun clearAll(options: MockKGateway.ClearOptions, currentThreadOnly: Boolean) {
        clearer.clearAll(options, currentThreadOnly)
    }

    fun isMock(cls: KClass<*>) = synchronized(handlers) {
        handlers[cls]?.constructorMockVariant != null
    }

    private fun <T : Any> getMockVariant(cls: KClass<T>): ConstructorMockVariant? {
        return synchronized(handlers) {
            handlers[cls]?.constructorMockVariant
        }
    }
}
