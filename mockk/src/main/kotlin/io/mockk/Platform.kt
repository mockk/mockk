package io.mockk

import kotlinx.coroutines.experimental.runBlocking
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.Collections.synchronizedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

actual object InternalPlatform {
    actual fun identityHashCode(obj: Any): Int = System.identityHashCode(obj)

    actual fun ref(obj: Any): Ref = JvmRef(obj)

    actual fun <T> runCoroutine(block: suspend () -> T): T {
        return runBlocking {
            block()
        }
    }

    actual fun Any?.toStr() =
            when (this) {
                null -> "null"
                is KClass<*> -> this.java.name
                is Method -> name + "(" + parameterTypes.map { it.simpleName }.joinToString() + ")"
                is Function<*> -> "lambda {}"
                else -> toString()
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

    actual fun <K, V> MutableMap<K, V>.customComputeIfAbsent(key: K, valueFunc: (K) -> V): V {
        val value = get(key)
        return if (value == null) {
            val newValue = valueFunc(key)
            put(key, newValue)
            newValue
        } else {
            value
        }
    }

    actual fun <T> synchronizedMutableList(): MutableList<T> {
        return synchronizedList(mutableListOf<T>())
    }

    actual fun <K, V> weakMap(): MutableMap<K, V> = WeakConcurrentMap<K, V>()
}


internal class JvmRef(override val value: Any) : Ref {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ref) return false
        return value === other.value
    }

    override fun hashCode(): Int = System.identityHashCode(value)
    override fun toString(): String = "Ref(${value::class.simpleName}@${hashCode()})"
}

internal class WeakConcurrentMap<K, V>() : MutableMap<K, V> {
    private val map = ConcurrentHashMap<Any, V>()
    private val queue = ReferenceQueue<K>()

    override fun get(key: K): V? {
        return map[StrongKey(key)]
    }

    override fun put(key: K, value: V): V? {
        expunge()
        return map.put(WeakKey(key, queue), value)
    }

    override fun remove(key: K): V? {
        expunge()
        return map.remove(StrongKey(key))
    }


    private fun expunge() {
        var ref: Reference<*>?
        ref = queue.poll()
        while (ref != null) {
            map.remove(ref)
            ref = queue.poll()
        }
    }

    private class WeakKey<K>(key: K, queue: ReferenceQueue<K>) : WeakReference<K>(key, queue) {
        private val hashCode: Int

        init {
            hashCode = System.identityHashCode(key)
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            } else {
                val key = get()
                if (key != null) {
                    if (other is WeakKey<*>) {
                        return key === other.get()
                    } else if (other is StrongKey<*>) {
                        return key === other.get()
                    }
                }
            }
            return false
        }

        override fun hashCode(): Int {
            return hashCode
        }
    }

    private class StrongKey<K>(private val key: K) {
        private val hashCode: Int

        init {
            hashCode = System.identityHashCode(key)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            } else {
                val key = get()
                if (key != null) {
                    if (other is WeakKey<*>) {
                        return key === other.get()
                    } else if (other is StrongKey<*>) {
                        return key === other.get()
                    }
                }
            }
            return false
        }

        override fun hashCode(): Int {
            return hashCode
        }

        fun get(): K? {
            return key
        }
    }


    override val size
        get() = map.size

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun containsKey(key: K): Boolean {
        return get(key) != null
    }

    override fun containsValue(value: V): Boolean {
        return map.containsValue(value)
    }

    override fun putAll(from: Map<out K, V>) {
        throw UnsupportedOperationException("putAll")
    }

    override fun clear() {
        map.clear()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = throw UnsupportedOperationException("entries")
    override val keys: MutableSet<K>
        get() = throw UnsupportedOperationException("entries")
    override val values: MutableCollection<V>
        get() = map.values
}
