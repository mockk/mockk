package io.mockk.impl

import io.mockk.MockKGateway.*
import io.mockk.MockKException
import io.mockk.external.logger
import javassist.ClassPool
import javassist.bytecode.ClassFile
import javassist.util.proxy.MethodFilter
import javassist.util.proxy.MethodHandler
import javassist.util.proxy.ProxyFactory
import javassist.util.proxy.ProxyObject
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

internal class InstantiatorImpl(private val gw: MockKGatewayImpl) : Instantiator {
    private val log = logger<InstantiatorImpl>()

    private val cp = ClassPool.getDefault()

    private val objenesis = ObjenesisStd()

    private val instantiators = mutableMapOf<KClass<*>, ObjectInstantiator<*>>()
    private val proxyClasses = mutableMapOf<ProxyClassSignature, KClass<*>>()

    private val instantiationFactories = mutableListOf<InstanceFactory>()

    private val rnd = Random()

    @Suppress("DEPRECATION")
    override fun <T : Any> proxy(cls: KClass<T>, useDefaultConstructor: Boolean, moreInterfaces: Array<out KClass<*>>): Any {
        log.trace { "Building proxy for ${cls.toStr()} hashcode=${Integer.toHexString(cls.hashCode())}" }

        try {
            val signature = ProxyClassSignature(cls, linkedSetOf(MockKInstance::class, *moreInterfaces))
            val proxyCls = proxyClasses.java6ComputeIfAbsent(signature) {
                ProxyFactoryExt(it).buildProxy(cls)
            }

            return if (useDefaultConstructor)
                proxyCls.java.newInstance()
            else
                newEmptyInstance(proxyCls)
        } catch (ex: Exception) {
            log.trace(ex) { "Failed to build proxy for ${cls.toStr()}. " +
                    "Trying just instantiate it. " +
                    "This can help if it's last call in the chain" }
            return instantiate(cls)
        }
    }


    override fun <T : Any> instantiate(cls: KClass<T>): T {
        log.trace { "Building empty instance ${cls.toStr()}" }

        val ret = if (!cls.isFinal) {
            try {
                instantiateViaProxy(cls)
            } catch (ex: Exception) {
                log.trace(ex) { "Failed to instantiate via proxy ${cls.toStr()}. " +
                        "Doing just instantiation" }
                newEmptyInstance(cls)
            }
        } else {
            newEmptyInstance(cls)
        }
        return cls.cast(ret)
    }

    private fun instantiateViaProxy(cls: KClass<*>): Any {

        for (factory in instantiationFactories) {
            val instance = factory.instantiate(cls)
            if (instance != null) {
                return instance
            }
        }

        val signature = ProxyClassSignature(cls, setOf())
        val proxyCls = proxyClasses.java6ComputeIfAbsent(signature, {
            ProxyFactoryExt(it).buildProxy(cls)
        })
        val instance = newEmptyInstance(proxyCls)
        (instance as ProxyObject).handler = EqualsAndHashCodeHandler()
        return instance
    }

    private fun <T : Any> ProxyFactoryExt.buildProxy(cls: KClass<T>): KClass<*> {
        return try {
            val classFile = buildClassFile()

            val proxyClass = cp.makeClass(classFile)

            proxyClass.toClass(cls.java.classLoader, cls.java.protectionDomain).kotlin

        } catch (ex: RuntimeException) {
            if (ex.message?.endsWith("is final") ?: false) {
                throw MockKException("Failed to create proxy for ${cls.toStr()}. Class is final. " +
                        "Put @MockKJUnit4Runner on your test or add MockK Java Agent instrumentation to make all classes 'open'", ex)
            }
            throw ex
        }
    }

    private class EqualsAndHashCodeHandler : MethodHandler {
        override fun invoke(self: Any, thisMethod: Method, proceed: Method?, args: Array<out Any>): Any? {
            return if (thisMethod.name == "hashCode" && thisMethod.parameterCount() == 0) {
                System.identityHashCode(self)
            } else if (thisMethod.name == "equals" &&
                    thisMethod.parameterCount() == 1 &&
                    thisMethod.parameterTypes[0] == java.lang.Object::class.java) {
                self === args[0]
            } else if (thisMethod.name == "toString" && thisMethod.parameterCount() == 0) {
                "instance<" + self.javaClass.superclass.simpleName + ">()"
            } else {
                null
            }
        }
    }

    private fun newEmptyInstance(proxyCls: KClass<*>): Any {
        val instantiator = instantiators.java6ComputeIfAbsent(proxyCls) { cls ->
            objenesis.getInstantiatorOf(cls.java)
        }
        return instantiator.newInstance()
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
}

data class ProxyClassSignature(val superclass: KClass<*>,
                               val interfaces: Set<KClass<*>>)

internal class ProxyFactoryExt(signature: ProxyClassSignature) : ProxyFactory() {
    init {
        val interfaceList = mutableListOf<KClass<*>>()
        if (signature.superclass.java.isInterface) {
            interfaceList.add(signature.superclass)
        } else {
            superclass = signature.superclass.java
        }
        interfaceList.addAll(signature.interfaces)
        interfaces = interfaceList.map { it.java }.toTypedArray()
    }

    fun buildClassFile(): ClassFile {
        try {
            computeSignatureMethod.invoke(this, MethodFilter { true })
            allocateClassNameMethod.invoke(this)
            return makeMethod.invoke(this) as ClassFile
        } catch (ex: InvocationTargetException) {
            throw ex.demangle()
        }
    }

    companion object {
        val makeMethod: Method = ProxyFactory::class.java.getDeclaredMethod("make")

        val computeSignatureMethod: Method = ProxyFactory::class.java.getDeclaredMethod("computeSignature",
                MethodFilter::class.java)

        val allocateClassNameMethod: Method = ProxyFactory::class.java.getDeclaredMethod("allocateClassName")

        init {
            makeMethod.isAccessible = true
            computeSignatureMethod.isAccessible = true
            allocateClassNameMethod.isAccessible = true
        }
    }
}

