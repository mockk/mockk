package io.mockk

import kotlinx.coroutines.experimental.runBlocking
import java.lang.reflect.Method
import kotlin.reflect.KClass

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

    actual fun dynamicCall(self: Any, methodName: String, args: Array<out Any?>): Any? {
        val method = self::class.java.declaredMethods.firstOrNull {
            it.name == methodName &&
                    it.parameters.size == args.size &&
                    it.parameters.zip(args).all { it.first.type.isInstance(it.second) }
        } ?: throw MockKException("can't find function $methodName(${args.joinToString(", ")}) for dynamic call")

        method.isAccessible = true
        return method.invoke(self, *args)

    }
}
