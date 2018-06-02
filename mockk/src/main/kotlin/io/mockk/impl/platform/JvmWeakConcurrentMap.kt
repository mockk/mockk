package io.mockk.impl.platform

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class JvmWeakConcurrentMap<K, V>() : MutableMap<K, V> {
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
        var ref = queue.poll()
        while (ref != null) {
            val value = map.remove(ref)
            if (value is Disposable) {
                value.dispose()
            }
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