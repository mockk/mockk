package io.mockk.impl.platform

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference

class JvmWeakConcurrentMapTest {
    /**
     * When GC clears a weak key, the entry can never be looked up again: both get() and
     * remove() match through the key's referent. removeIf() used to skip such entries,
     * so clear() could leave values behind in the map. That race is what made
     * StubRepositoryTest fail intermittently on CI: clearAllStubsFromMemory() returned
     * with stale entries still present and allStubs kept counting their stubs.
     *
     * WeakReference.clear() reproduces the exact window deterministically: the referent
     * is dropped without the reference ever being enqueued, so expunge() cannot see it.
     */
    @Test
    fun `removeIf removes entries whose weak key was cleared`() {
        val map = JvmWeakConcurrentMap<Any, Any>()

        val key = Any()
        map.put(key, "value")

        val weakKey = storedWeakKey(map, key)
        weakKey.clear()

        map.removeIf { _, _ -> true }

        assertTrue(map.isEmpty()) { "entries with a cleared weak key must be removed, still: ${map.values}" }
    }

    private fun storedWeakKey(
        map: JvmWeakConcurrentMap<Any, Any>,
        key: Any,
    ): WeakReference<*> {
        val field = JvmWeakConcurrentMap::class.java.getDeclaredField("map")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val inner = field.get(map) as Map<Any, Any>
        val weakKey =
            inner.keys.singleOrNull { it is WeakReference<*> && it.get() === key }
        checkNotNull(weakKey) { "expected a weak entry stored for the given key" }
        return weakKey as WeakReference<*>
    }
}
