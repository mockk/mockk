package io.mockk.impl.platform

import io.mockk.impl.InternalPlatform
import io.mockk.impl.Ref

class CommonIdentityHashMapOf<K, V> : MutableMap<K, V> {
    val map = linkedMapOf<Ref?, V>()

    override val size: Int
        get() = map.size

    override fun containsKey(key: K): Boolean {
        return map.containsKey(ref(key))
    }

    override fun containsValue(value: V): Boolean {
        return map.containsValue(value)
    }

    override fun get(key: K): V? {
        return map.get(ref(key))
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = throw UnsupportedOperationException("entries")

    override val keys: MutableSet<K>
        get() = throw UnsupportedOperationException("keys")

    override val values: MutableCollection<V>
        get() = map.values

    override fun clear() {
        map.clear()
    }

    override fun put(key: K, value: V): V? {
        return map.put(ref(key), value)
    }

    override fun putAll(from: Map<out K, V>) {
        throw UnsupportedOperationException("putAll")
    }

    override fun remove(key: K): V? {
        return map.remove(ref(key))
    }

    private fun ref(key: K) = if (key == null) null else InternalPlatform.ref(key)
}