package io.mockk.impl.platform

import io.mockk.impl.InternalPlatform.ref
import io.mockk.Ref

class JsIdentityHashMapOf<K : Any, V> : MutableMap<K, V> {
    val map = hashMapOf<Ref, V>()
    
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
        get() = TODO("not implemented")

    override val keys: MutableSet<K>
        get() = TODO("not implemented")

    override val values: MutableCollection<V>
        get() = map.values

    override fun clear() {
        map.clear()
    }

    override fun put(key: K, value: V): V? {
        return map.put(ref(key), value)
    }

    override fun putAll(from: Map<out K, V>) {
        TODO("not implemented")
    }

    override fun remove(key: K): V? {
        return map.remove(ref(key))
    }
}