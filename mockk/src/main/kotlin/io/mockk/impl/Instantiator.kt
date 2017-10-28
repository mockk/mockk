package io.mockk.impl

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

internal class InstantiatorImpl(private val gw: MockKGatewayImpl) : io.mockk.Instantiator {
    private val log = logger<InstantiatorImpl>()

    private val cp = ClassPool.getDefault()

    private val objenesis = ObjenesisStd()

    private val instantiators = mutableMapOf<Class<*>, ObjectInstantiator<*>>()

    private val rnd = Random()
//    private val noArgsType = Class.forName(MockKGateway.NO_ARG_TYPE_NAME)

    @Suppress("DEPRECATION")
    override fun <T> proxy(cls: Class<T>, useDefaultConstructor: Boolean): Any {
        log.trace { "Building proxy for $cls hashcode=${Integer.toHexString(cls.hashCode())}" }

        val pf = ProxyFactoryExt(cls, MockKInstance::class.java)

        val proxyCls = try {
            cp.makeClass(pf.buildClassFile()).toClass(cls.classLoader, cls.protectionDomain)
        } catch(ex: RuntimeException) {
            if (ex.message?.endsWith("is final") ?: false) {
                throw MockKException("Failed to create proxy for $cls. Class is final. " +
                        "You need MockK Java agent to make all classes 'open'")
            }
            throw ex
        }

        return if (useDefaultConstructor)
            proxyCls.newInstance()
        else
            newEmptyInstance(proxyCls)
    }


    override fun <T> instantiate(cls: Class<T>): T {
        log.trace { "Building empty instance $cls" }
        val pf = ProxyFactoryExt(cls)
        val proxyCls = cp.makeClass(pf.buildClassFile()).toClass(cls.classLoader, cls.protectionDomain)
        val instance = newEmptyInstance(proxyCls)
        (instance as ProxyObject).handler = EqualsAndHashCodeHandler()
        return cls.cast(instance)
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

    private fun newEmptyInstance(proxyCls: Class<*>): Any {
        val instantiator = instantiators.computeIfAbsent(proxyCls) { cls ->
            objenesis.getInstantiatorOf(cls)
        }
        return instantiator.newInstance()
    }

    override fun anyValue(cls: Class<*>, orInstantiateVia: () -> Any?): Any? {
        return when (cls) {
            Void.TYPE -> Unit

            Boolean::class.java -> false
            Byte::class.java -> 0.toByte()
            Short::class.java -> 0.toShort()
            Char::class.java -> 0.toChar()
            Int::class.java -> 0
            Long::class.java -> 0L
            Float::class.java -> 0.0F
            Double::class.java -> 0.0
            String::class.java -> ""

            java.lang.Boolean::class.java -> false
            java.lang.Byte::class.java -> 0.toByte()
            java.lang.Short::class.java -> 0.toShort()
            java.lang.Character::class.java -> 0.toChar()
            java.lang.Integer::class.java -> 0
            java.lang.Long::class.java -> 0L
            java.lang.Float::class.java -> 0.0F
            java.lang.Double::class.java -> 0.0

            BooleanArray::class.java -> BooleanArray(0)
            ByteArray::class.java -> ByteArray(0)
            CharArray::class.java -> CharArray(0)
            ShortArray::class.java -> ShortArray(0)
            IntArray::class.java -> IntArray(0)
            LongArray::class.java -> LongArray(0)
            FloatArray::class.java -> FloatArray(0)
            DoubleArray::class.java -> DoubleArray(0)
            else -> {
                if (cls.isArray) {
                    java.lang.reflect.Array.newInstance(cls.componentType, 0)
                } else {
                    orInstantiateVia()
                }
            }
        }
    }

    override fun <T> signatureValue(cls: Class<T>): T {
        return cls.cast(when (cls) {
            java.lang.Boolean::class.java -> rnd.nextBoolean()
            java.lang.Byte::class.java -> rnd.nextInt().toByte()
            java.lang.Short::class.java -> rnd.nextInt().toShort()
            java.lang.Character::class.java -> rnd.nextInt().toChar()
            java.lang.Integer::class.java -> rnd.nextInt()
            java.lang.Long::class.java -> rnd.nextLong()
            java.lang.Float::class.java -> rnd.nextFloat()
            java.lang.Double::class.java -> rnd.nextDouble()
            java.lang.String::class.java -> rnd.nextLong().toString(16)
//            java.lang.Object::class.java -> java.lang.Object()
            else -> instantiate(cls)
        })
    }

    override fun isPassedByValue(cls: Class<*>): Boolean {
        return when (cls) {
            java.lang.Boolean::class.java -> true
            java.lang.Byte::class.java -> true
            java.lang.Short::class.java -> true
            java.lang.Character::class.java -> true
            java.lang.Integer::class.java -> true
            java.lang.Long::class.java -> true
            java.lang.Float::class.java -> true
            java.lang.Double::class.java -> true
            java.lang.String::class.java -> true
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
        return when (obj1.javaClass) {
            BooleanArray::class.java -> Arrays.equals(obj1 as BooleanArray, obj2 as BooleanArray)
            ByteArray::class.java -> Arrays.equals(obj1 as ByteArray, obj2 as ByteArray)
            CharArray::class.java -> Arrays.equals(obj1 as CharArray, obj2 as CharArray)
            ShortArray::class.java -> Arrays.equals(obj1 as ShortArray, obj2 as ShortArray)
            IntArray::class.java -> Arrays.equals(obj1 as IntArray, obj2 as IntArray)
            LongArray::class.java -> Arrays.equals(obj1 as LongArray, obj2 as LongArray)
            FloatArray::class.java -> Arrays.equals(obj1 as FloatArray, obj2 as FloatArray)
            DoubleArray::class.java -> Arrays.equals(obj1 as DoubleArray, obj2 as DoubleArray)
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
}

internal class ProxyFactoryExt(cls: Class<*>, vararg additionalInterfaces: Class<*>) : ProxyFactory() {
    init {
        if (cls.isInterface) {
            val interfaceList = additionalInterfaces.toMutableList()
            interfaceList.add(cls)
            interfaces = interfaceList.toTypedArray()
        } else {
            superclass = cls
            interfaces = additionalInterfaces
        }
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

