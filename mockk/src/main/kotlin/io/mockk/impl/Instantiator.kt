package io.mockk.impl

import io.mockk.MockKException
import io.mockk.MockKGateway.*
import io.mockk.agent.MockKAgentException
import io.mockk.external.logger
import io.mockk.proxy.MockKProxyMaker
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Callable
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

internal class InstantiatorImpl(private val gateway: MockKGatewayImpl) : Instantiator {
    private val instantiationFactories = mutableListOf<InstanceFactory>()

    private val rnd = Random()

    @Suppress("DEPRECATION")
    override fun <T : Any> proxy(cls: KClass<T>,
                                 useDefaultConstructor: Boolean,
                                 instantiateOnFailure: Boolean,
                                 moreInterfaces: Array<out KClass<*>>,
                                 stub: Stub): Any {
        log.trace { "Building proxy for ${cls.toStr()} hashcode=${Integer.toHexString(cls.hashCode())}" }

        try {
            return MockKProxyMaker.INSTANCE.proxy(
                    cls.java,
                    moreInterfaces.map { it.java }.toTypedArray(),
                    { self, method, originalCall, args ->
                        stdClassFunctions(self, method, args) {
                            stub.handleInvocation(self, method.toDescription(), {
                                handleOriginalCall(originalCall, method)
                            }, args)
                        }
                    },
                    useDefaultConstructor)
        } catch (ex: MockKAgentException) {
            if (!instantiateOnFailure) {
                if (useDefaultConstructor) {
                    throw MockKException("Can't instantiate proxy via default constructor for " + cls, ex)
                } else {
                    throw MockKException("Can't instantiate proxy for " + cls, ex)
                }
            }
            log.trace(ex) {
                "Failed to build proxy for ${cls.toStr()}. " +
                        "Trying just instantiate it. " +
                        "This can help if it's last call in the chain"
            }
            return instantiate(cls)
        }
    }

    private fun handleOriginalCall(originalMethod: Callable<*>?, method: Method): Any? {
        if (originalMethod == null) {
            throw MockKException("No way to call original method ${method.toDescription()}")
        }

        return try {
            originalMethod.call()
        } catch (ex: InvocationTargetException) {
            throw MockKException("Failed to execute original call. Check cause please", ex.cause)
        }
    }

    override fun <T : Any> instantiate(cls: KClass<T>): T {
        log.trace { "Building empty instance ${cls.toStr()}" }

        for (factory in instantiationFactories) {
            val instance = factory.instantiate(cls)
            if (instance != null) {
                log.trace { "Instance factory returned instance $instance" }
                return cls.cast(instance)
            }
        }

        return MockKProxyMaker.INSTANCE.instance(cls.java)
    }

    override fun anyValue(cls: KClass<*>, orInstantiateVia: () -> Any?): Any? {
        return when (cls) {
            Void.TYPE.kotlin -> Unit

            Boolean::class -> false
            Byte::class -> 0.toByte()
            Short::class -> 0.toShort()
            Char::class -> 0.toChar()
            Int::class -> 0
            Long::class -> 0L
            Float::class -> 0.0F
            Double::class -> 0.0
            String::class -> ""

            java.lang.Boolean::class -> false
            java.lang.Byte::class -> 0.toByte()
            java.lang.Short::class -> 0.toShort()
            java.lang.Character::class -> 0.toChar()
            java.lang.Integer::class -> 0
            java.lang.Long::class -> 0L
            java.lang.Float::class -> 0.0F
            java.lang.Double::class -> 0.0

            BooleanArray::class -> BooleanArray(0)
            ByteArray::class -> ByteArray(0)
            CharArray::class -> CharArray(0)
            ShortArray::class -> ShortArray(0)
            IntArray::class -> IntArray(0)
            LongArray::class -> LongArray(0)
            FloatArray::class -> FloatArray(0)
            DoubleArray::class -> DoubleArray(0)
            else -> {
                if (cls.java.isArray) {
                    java.lang.reflect.Array.newInstance(cls.java.componentType, 0)
                } else {
                    orInstantiateVia()
                }
            }
        }
    }

    override fun <T : Any> signatureValue(cls: KClass<T>): T {
        return cls.cast(when (cls) {
            java.lang.Boolean::class -> rnd.nextBoolean()
            java.lang.Byte::class -> rnd.nextInt().toByte()
            java.lang.Short::class -> rnd.nextInt().toShort()
            java.lang.Character::class -> rnd.nextInt().toChar()
            java.lang.Integer::class -> rnd.nextInt()
            java.lang.Long::class -> rnd.nextLong()
            java.lang.Float::class -> rnd.nextFloat()
            java.lang.Double::class -> rnd.nextDouble()
            java.lang.String::class -> rnd.nextLong().toString(16)
//            java.lang.Object::class -> java.lang.Object()
            else -> instantiate(cls)
        })
    }

    override fun isPassedByValue(cls: KClass<*>): Boolean {
        return when (cls) {
            java.lang.Boolean::class -> true
            java.lang.Byte::class -> true
            java.lang.Short::class -> true
            java.lang.Character::class -> true
            java.lang.Integer::class -> true
            java.lang.Long::class -> true
            java.lang.Float::class -> true
            java.lang.Double::class -> true
            java.lang.String::class -> true
            else -> false
        }
    }

    /**
     * Java 6 complaint deep equals
     */
    override fun deepEquals(obj1: Any?, obj2: Any?): Boolean {
        return if (obj1 === obj2) {
            true
        } else if (obj1 == null || obj2 == null) {
            obj2 == null
        } else if (obj1.javaClass != obj2.javaClass) {
            false
        } else if (obj1.javaClass.isArray) {
            arrayDeepEquals(obj1, obj2)
        } else {
            obj1 == obj2
        }
    }

    private fun arrayDeepEquals(obj1: Any, obj2: Any): Boolean {
        return when (obj1::class) {
            BooleanArray::class -> Arrays.equals(obj1 as BooleanArray, obj2 as BooleanArray)
            ByteArray::class -> Arrays.equals(obj1 as ByteArray, obj2 as ByteArray)
            CharArray::class -> Arrays.equals(obj1 as CharArray, obj2 as CharArray)
            ShortArray::class -> Arrays.equals(obj1 as ShortArray, obj2 as ShortArray)
            IntArray::class -> Arrays.equals(obj1 as IntArray, obj2 as IntArray)
            LongArray::class -> Arrays.equals(obj1 as LongArray, obj2 as LongArray)
            FloatArray::class -> Arrays.equals(obj1 as FloatArray, obj2 as FloatArray)
            DoubleArray::class -> Arrays.equals(obj1 as DoubleArray, obj2 as DoubleArray)
            else -> {
                val arr1 = obj1 as Array<*>
                val arr2 = obj2 as Array<*>
                if (arr1.size != arr2.size) {
                    return false
                }
                repeat(arr1.size) { i ->
                    if (!deepEquals(arr1[i], arr2[i])) {
                        return false
                    }
                }
                return true
            }
        }
    }

    override fun registerFactory(factory: InstanceFactory) {
        instantiationFactories.add(factory)
    }

    override fun unregisterFactory(factory: InstanceFactory) {
        instantiationFactories.remove(factory)
    }

    override fun staticMockk(cls: KClass<*>, stub: Stub) {
        log.trace { "Building static proxy for ${cls.toStr()} hashcode=${Integer.toHexString(cls.hashCode())}" }

        try {
            return MockKProxyMaker.INSTANCE.staticProxy(cls.java,
                    { self, method, originalMethod, args ->
                        stdClassFunctions(self, method, args) {
                            stub.handleInvocation(self, method.toDescription(), {
                                handleOriginalCall(originalMethod, method)
                            }, args)
                        }
                    })
        } catch (ex: MockKAgentException) {
            throw MockKException("Failed to build static proxy", ex)
        }
    }

    override fun staticUnMockk(cls: KClass<*>) {
        MockKProxyMaker.INSTANCE.staticUnProxy(cls.java)
    }

    protected inline fun stdClassFunctions(self: Any,
                                           method: Method,
                                           args: Array<Any?>,
                                           otherwise: () -> Any?): Any? {
        if (self is Class<*>) {
            if (method.isHashCode()) {
                return System.identityHashCode(self)
            } else if (method.isEquals()) {
                return self === args[0]
            }
        }
        return otherwise()
    }

    companion object {
        val log = logger<InstantiatorImpl>()
    }
}

private fun Method.isHashCode() = name == "hashCode" && parameterTypes.isEmpty()

private fun Method.isEquals() = name == "equals" && parameterTypes.size == 1 && parameterTypes[0] === Object::class.java
