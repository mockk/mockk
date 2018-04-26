package io.mockk

import kotlinx.coroutines.experimental.runBlocking
import java.lang.reflect.Method
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaSetter

actual object InternalPlatformDsl {
    actual fun identityHashCode(obj: Any): Int = System.identityHashCode(obj)

    actual fun <T> runCoroutine(block: suspend () -> T): T {
        return runBlocking {
            block()
        }
    }

    actual fun Any?.toStr() =
        try {
            when (this) {
                null -> "null"
                is BooleanArray -> this.contentToString()
                is ByteArray -> this.contentToString()
                is CharArray -> this.contentToString()
                is ShortArray -> this.contentToString()
                is IntArray -> this.contentToString()
                is LongArray -> this.contentToString()
                is FloatArray -> this.contentToString()
                is DoubleArray -> this.contentToString()
                is Array<*> -> this.contentDeepToString()
                Void.TYPE.kotlin -> "void"
                is KClass<*> -> this.simpleName ?: "<null name class>"
                is Method -> name + "(" + parameterTypes.map { it.simpleName }.joinToString() + ")"
                is Function<*> -> "lambda {}"
                else -> toString()
            }
        } catch (thr: Throwable) {
            "<error \"$thr\">"
        }

    actual fun deepEquals(obj1: Any?, obj2: Any?): Boolean {
        return if (obj1 === obj2) {
            true
        } else if (obj1 == null || obj2 == null) {
            obj1 === obj2
        } else if (obj1.javaClass.isArray && obj2.javaClass.isArray) {
            arrayDeepEquals(obj1, obj2)
        } else {
            obj1 == obj2
        }
    }

    private fun arrayDeepEquals(obj1: Any, obj2: Any): Boolean {
        return when (obj1) {
            is BooleanArray -> obj1 contentEquals obj2 as BooleanArray
            is ByteArray -> obj1 contentEquals obj2 as ByteArray
            is CharArray -> obj1 contentEquals obj2 as CharArray
            is ShortArray -> obj1 contentEquals obj2 as ShortArray
            is IntArray -> obj1 contentEquals obj2 as IntArray
            is LongArray -> obj1 contentEquals obj2 as LongArray
            is FloatArray -> obj1 contentEquals obj2 as FloatArray
            is DoubleArray -> obj1 contentEquals obj2 as DoubleArray
            else -> return obj1 as Array<*> contentDeepEquals obj2 as Array<*>
        }
    }

    actual fun unboxChar(value: Any): Any = value

    actual fun Any.toArray(): Array<*> =
        when (this) {
            is BooleanArray -> this.toTypedArray()
            is ByteArray -> this.toTypedArray()
            is CharArray -> this.toTypedArray()
            is ShortArray -> this.toTypedArray()
            is IntArray -> this.toTypedArray()
            is LongArray -> this.toTypedArray()
            is FloatArray -> this.toTypedArray()
            is DoubleArray -> this.toTypedArray()
            else -> this as Array<*>
        }

    actual fun classForName(name: String): Any = Class.forName(name).kotlin

    actual fun dynamicCall(
        self: Any,
        methodName: String,
        args: Array<out Any?>,
        anyContinuationGen: () -> Continuation<*>
    ): Any? {
        val params = arrayOf(self, *args)
        val func = self::class.functions.firstOrNull {
            it.name == methodName &&
                    it.parameters.size == params.size &&
                    it.parameters.zip(params).all {
                        val classifier = it.first.type.classifier
                        if (classifier is KClass<*>) {
                            classifier.isInstance(it.second)
                        } else {
                            false
                        }
                    }
        } ?: throw MockKException("can't find function $methodName(${args.joinToString(", ")}) for dynamic call")

        func.javaMethod?.isAccessible = true
        if (func.isSuspend) {
            return func.call(*params, anyContinuationGen())
        } else {
            return func.call(*params)
        }
    }

    actual fun dynamicGet(self: Any, name: String): Any? {
        val property = self::class.memberProperties
            .filterIsInstance<KProperty1<Any, Any?>>()
            .firstOrNull {
                it.name == name
            } ?: throw MockKException("can't find property $name for dynamic property get")

        property.javaGetter?.isAccessible = true
        return property.get(self)
    }

    actual fun dynamicSet(self: Any, name: String, value: Any?) {
        val property = self::class.memberProperties
            .filterIsInstance<KMutableProperty1<Any, Any?>>()
            .firstOrNull {
                it.name == name
            } ?: throw MockKException("can't find property $name for dynamic property set")

        property.javaSetter?.isAccessible = true
        return property.set(self, value)
    }

}
