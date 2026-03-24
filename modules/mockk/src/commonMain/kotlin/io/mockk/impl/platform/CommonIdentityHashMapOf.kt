package io.mockk.impl.platform

import io.mockk.impl.InternalPlatform
import io.mockk.impl.Ref
import io.mockk.impl.WeakMap

class CommonIdentityHashMapOf<K, V> : WeakMap<K, V> {
    val map = linkedMapOf<Ref?, V>()

    override val size: Int
        get() = map.size

    override fun containsKey(key: K): Boolean = map.containsKey(ref(key))

    override fun containsValue(value: V): Boolean = map.containsValue(value)

    override fun get(key: K): V? = map[ref(key)]

    override fun isEmpty(): Boolean = map.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = throw UnsupportedOperationException("entries")

    override val keys: MutableSet<K>
        get() = throw UnsupportedOperationException("keys")

    override val values: MutableCollection<V>
        get() = map.values

    override fun clear() = removeIf { _, _ -> true }

    override fun put(
        key: K,
        value: V,
    ): V? = map.put(ref(key), value)

    override fun putAll(from: Map<out K, V>): Unit = throw UnsupportedOperationException("putAll")

    override fun remove(key: K): V? = map.remove(ref(key))

    override fun removeIf(predicate: (K, V) -> Boolean) {
        var rootException: Throwable? = null
        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val ref = entry.key ?: continue

            @Suppress("UNCHECKED_CAST")
            val key = ref.value as K
            val value = entry.value

            if (predicate(key, value)) {
                iterator.remove()

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
        rootException?.let { throw it }
    }

    private fun ref(key: K) = if (key == null) null else InternalPlatform.ref(key)
}
