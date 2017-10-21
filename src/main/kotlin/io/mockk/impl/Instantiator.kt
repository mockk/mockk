package io.mockk.impl

import io.mockk.Instantiator
import io.mockk.MockKException
import io.mockk.logger
import javassist.ClassPool
import javassist.bytecode.ClassFile
import javassist.util.proxy.MethodFilter
import javassist.util.proxy.MethodHandler
import javassist.util.proxy.ProxyFactory
import javassist.util.proxy.ProxyObject
import sun.reflect.ReflectionFactory
import java.lang.reflect.Method
import java.util.*

internal class InstantiatorImpl(gw: MockKGatewayImpl) : Instantiator {
    private val log = logger<InstantiatorImpl>()

    private val cp = ClassPool.getDefault()

    private val rnd = Random()
//    private val noArgsType = Class.forName(MockKGateway.NO_ARG_TYPE_NAME)

    override fun <T> proxy(cls: Class<T>, useDefaultConstructor: Boolean): Any {
        log.debug { "Building proxy for $cls" }

        val pf = ProxyFactoryExt(cls, MockKInstance::class.java)

        val proxyCls = cp.makeClass(pf.buildClassFile()).toClass()

        return if (useDefaultConstructor)
            proxyCls.newInstance()
        else
            newEmptyInstance(proxyCls)
    }


    override fun <T> instantiate(cls: Class<T>): T {
        log.debug { "Building empty instance $cls" }
        val pf = ProxyFactoryExt(cls)
        val proxyCls = cp.makeClass(pf.buildClassFile()).toClass()
        val instance = newEmptyInstance(proxyCls)
        (instance as ProxyObject).handler = EqualsAndHashCodeHandler()
        return cls.cast(instance)
    }

    private class EqualsAndHashCodeHandler : MethodHandler {
        override fun invoke(self: Any, thisMethod: Method, proceed: Method?, args: Array<out Any>): Any? {
            return if (thisMethod.name == "hashCode" && thisMethod.parameterCount == 0) {
                System.identityHashCode(self)
            } else if (thisMethod.name == "equals" &&
                    thisMethod.parameterCount == 1 &&
                    thisMethod.parameterTypes[0] == java.lang.Object::class.java) {
                self === args[0]
            } else if (thisMethod.name == "toString" && thisMethod.parameterCount == 0) {
                self.javaClass.superclass.name + "@" + System.identityHashCode(self)
            } else {
                null
            }
        }
    }

    val reflectionFactoryFinder =
            try {
                Class.forName("sun.reflect.ReflectionFactory")
                ReflecationFactoryFinder()
            } catch (cnf: ClassNotFoundException) {
                null
            }

    private fun newEmptyInstance(proxyCls: Class<*>): Any {
//                    factory.create(arrayOf(noArgsType), arrayOf<Any?>(null))

        // TODO : use objenesis
        reflectionFactoryFinder?.let { return it.newEmptyInstance(proxyCls) }
        throw MockKException("no instantiation support on platform")
    }

    override fun anyValue(type: Class<*>, orInstantiateVia: () -> Any?): Any? {
        return when (type) {
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
                if (type.isArray) {
                    java.lang.reflect.Array.newInstance(type.componentType, 0);
                } else {
                    orInstantiateVia()
                }
            }
        }
    }

    override fun <T> signatureValue(cls: Class<T>): T {
        return cls.cast(when (cls) {
            java.lang.Boolean::class.java -> java.lang.Boolean(rnd.nextBoolean())
            java.lang.Byte::class.java -> java.lang.Byte(rnd.nextInt().toByte())
            java.lang.Short::class.java -> java.lang.Short(rnd.nextInt().toShort())
            java.lang.Character::class.java -> java.lang.Character(rnd.nextInt().toChar())
            java.lang.Integer::class.java -> java.lang.Integer(rnd.nextInt())
            java.lang.Long::class.java -> java.lang.Long(rnd.nextLong())
            java.lang.Float::class.java -> java.lang.Float(rnd.nextFloat())
            java.lang.Double::class.java -> java.lang.Double(rnd.nextDouble())
            java.lang.String::class.java -> java.lang.String(rnd.nextLong().toString(16))
            java.lang.Object::class.java -> java.lang.Object()
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

    class ProxyFactoryExt(cls: Class<*>, vararg intfs: Class<*>) : ProxyFactory() {
        init {
            if (cls.isInterface) {
                val intfs = intfs.toMutableList()
                intfs.add(cls)
                interfaces = intfs.toTypedArray()
            } else {
                superclass = cls
                interfaces = intfs
            }
        }

        fun buildClassFile(): ClassFile {
            computeSignatureMethod.invoke(this, MethodFilter { true })
            allocateClassNameMethod.invoke(this)
            return makeMethod.invoke(this) as ClassFile
        }

        companion object {
            val makeMethod = ProxyFactory::class.java.getDeclaredMethod("make")

            val computeSignatureMethod = ProxyFactory::class.java.getDeclaredMethod("computeSignature",
                    MethodFilter::class.java)

            val allocateClassNameMethod = ProxyFactory::class.java.getDeclaredMethod("allocateClassName")

            init {
                makeMethod.isAccessible = true
                computeSignatureMethod.isAccessible = true
                allocateClassNameMethod.isAccessible = true
            }
        }
    }

}

internal class ReflecationFactoryFinder {
    fun newEmptyInstance(proxyCls: Class<*>): Any {
        val rf = ReflectionFactory.getReflectionFactory();
        val objDef = Object::class.java.getDeclaredConstructor();
        val intConstr = rf.newConstructorForSerialization(proxyCls, objDef)
        return intConstr.newInstance()
    }
}
