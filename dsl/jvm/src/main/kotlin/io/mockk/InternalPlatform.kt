package io.mockk

import io.mockk.jvm.JvmRef
import io.mockk.jvm.WeakConcurrentMap
import kotlinx.coroutines.experimental.runBlocking
import java.lang.reflect.Method
import java.util.*
import java.util.Collections.synchronizedList
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object InternalPlatform {
    fun identityHashCode(obj: Any): Int = System.identityHashCode(obj)

    fun nanoTime() = System.nanoTime()

    fun ref(obj: Any): Ref = JvmRef(obj)

    fun <T> runCoroutine(block: suspend () -> T): T {
        return runBlocking {
            block()
        }
    }

    fun Any?.toStr() =
            when (this) {
                null -> "null"
                is KClass<*> -> this.java.name
                is Method -> name + "(" + parameterTypes.map { it.simpleName }.joinToString() + ")"
                is Function<*> -> "lambda {}"
                else -> toString()
            }

    fun hkd(obj: Any): String = Integer.toUnsignedString(identityHashCode(obj), 16)

    fun isPassedByValue(cls: KClass<*>): Boolean {
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

    fun deepEquals(obj1: Any?, obj2: Any?): Boolean {
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

    fun <K, V> MutableMap<K, V>.customComputeIfAbsent(key: K, valueFunc: (K) -> V): V {
        val value = get(key)
        return if (value == null) {
            val newValue = valueFunc(key)
            put(key, newValue)
            newValue
        } else {
            value
        }
    }

    fun <T> synchronizedMutableList(): MutableList<T> {
        return synchronizedList(mutableListOf<T>())
    }

    fun <K, V> weakMap(): MutableMap<K, V> = WeakConcurrentMap<K, V>()

    fun counter(): () -> Long = AtomicLong()::incrementAndGet

    fun packRef(arg: Any?): Any? {
        return if (arg == null || isPassedByValue(arg::class))
            arg
        else
            ref(arg)
    }

    fun isSuspend(paramTypes: List<KClass<*>>): Boolean {
        val sz = paramTypes.size
        if (sz == 0) {
            return false
        }
        return paramTypes[sz - 1].isSubclassOf(Continuation::class)
    }

    fun prettifyRecordingException(ex: Throwable): Throwable {
        throw when {
            ex is ClassCastException ->
                MockKException("Class cast exception. " +
                        "Probably type information was erased.\n" +
                        "In this case use `hint` before call to specify " +
                        "exact return type of a method. ", ex)

            ex is NoClassDefFoundError &&
                    ex.message?.contains("kotlinx/coroutines/") ?: false ->
                MockKException("Add coroutines support artifact 'org.jetbrains.kotlinx:kotlinx-coroutines-core' to your project ")

            else -> ex
        }
    }

    fun <T> synchronizedList(): MutableList<T> = Collections.synchronizedList(mutableListOf())

    fun <K, V> synchronizedMap(): MutableMap<K, V> = Collections.synchronizedMap(hashMapOf())
}
