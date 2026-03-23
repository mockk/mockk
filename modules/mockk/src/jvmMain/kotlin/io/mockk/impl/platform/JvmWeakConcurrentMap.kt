package io.mockk.impl.platform

import io.mockk.impl.WeakMap
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class JvmWeakConcurrentMap<K, V> : WeakMap<K, V> {
    private val map = ConcurrentHashMap<Any, V>()
    private val queue = ReferenceQueue<K>()

    override fun get(key: K): V? = map[StrongKey(key)]

    override fun put(
        key: K,
        value: V,
    ): V? {
        expunge()
        return if (value != null) {
            map.put(WeakKey(key, queue), value)
        } else {
            null
        }
    }

    override fun remove(key: K): V? {
        expunge()
        return map.remove(StrongKey(key))
    }

    private fun expunge() {
        var rootException: Throwable? = null
        var ref = queue.poll()
        while (ref != null) {
            val value = map.remove(ref)
            if (value is Disposable) {
                try {
                    value.dispose()
                } catch (e: Throwable) {
                    if (rootException == null) {
                        rootException = e
                    } else {
                        rootException.addSuppressed(e)
                    }
                }
            }
            ref = queue.poll()
        }
        rootException?.let { throw it }
    }

    private class WeakKey<K>(
        key: K,
        queue: ReferenceQueue<K>,
    ) : WeakReference<K>(key, queue) {
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

        override fun hashCode(): Int = hashCode
    }

    private class StrongKey<K>(
        private val key: K,
    ) {
        private val hashCode: Int = System.identityHashCode(key)

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

        override fun hashCode(): Int = hashCode

        fun get(): K? = key
    }

    override val size
        get() = map.size

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun containsKey(key: K): Boolean = get(key) != null

    override fun containsValue(value: V): Boolean = map.containsValue(value)

    override fun putAll(from: Map<out K, V>): Unit = throw UnsupportedOperationException("putAll")

    override fun clear() = removeIf { _, _ -> true }

    override fun removeIf(predicate: (K, V) -> Boolean) {
        expunge()

        var rootException: Throwable? = null
        for (entity in map.entries) {
            @Suppress("UNCHECKED_CAST")
            val weakKey = entity.key as? WeakKey<K>
            val k = weakKey?.get() ?: continue
            val v = entity.value
            if (predicate(k, v)) {
                val value = map.remove(weakKey)
                if (value != null) {
                    if (value is Disposable) {
                        try {
                            value.dispose()
                        } catch (e: Throwable) {
                            if (rootException == null) {
                                rootException = e
                            } else {
                                rootException.addSuppressed(e)
                            }
                        }
                    }
                }
            }
        }
        rootException?.let { throw it }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = throw UnsupportedOperationException("entries")
    override val keys: MutableSet<K>
        get() = throw UnsupportedOperationException("entries")
    override val values: MutableCollection<V>
        get() = map.values
}
