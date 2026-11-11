package io.mockk.it

import io.mockk.clearAllMocks
import io.mockk.clearAllStubsFromMemory
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.util.assertGarbageCollected
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference

class ProxyWeakReferenceTest {
    @Test
    fun clearMocksFreesSpiesHoldingLargeObjects() {
        for (i in 0..100) {
            val spyk = spyk(LazySpringBeanWhichHoldsReferenceForBeanFactory(ByteArray(100 * 1024 * 1024)))
            every { spyk.doSmth() } returns "Wow"
            spyk.doSmth()
            clearMocks(spyk)
        }
    }

    @Test
    fun clearAllMocksLetsMocksBeGarbageCollected() {
        val refs = createMocks(100)
        clearAllMocks()
        assertGarbageCollected(refs, "mocks that were never garbage collected")
    }

    @Test
    fun clearAllMocksLetsRelaxedMocksAndTheirChildMocksBeGarbageCollected() {
        val refs =
            List(100) {
                val parent = mockk<Parent>(relaxed = true)
                val child = parent.child()
                child.value()
                listOf(WeakReference<Any>(parent), WeakReference<Any>(child))
            }.flatten()
        clearAllMocks()
        assertGarbageCollected(refs, "relaxed mocks or child mocks that were never garbage collected")
    }

    @Test
    fun clearAllStubsFromMemoryFreesSpiesHoldingLargeObjects() {
        for (i in 0..100) {
            val spyk = spyk(LazySpringBeanWhichHoldsReferenceForBeanFactory(ByteArray(100 * 1024 * 1024)))
            every { spyk.doSmth() } returns "Wow"
            spyk.doSmth()
            clearAllStubsFromMemory()
        }
    }

    @Test
    fun clearAllStubsFromMemoryLetsMocksBeGarbageCollected() {
        val refs = createMocks(100)
        clearAllStubsFromMemory()
        assertGarbageCollected(refs, "mocks that were never garbage collected")
    }

    private fun createMocks(count: Int): List<WeakReference<Dependency>> =
        List(count) {
            val mock = mockk<Dependency>()
            every { mock.value() } returns "x"
            mock.value()
            WeakReference(mock)
        }

    interface Dependency {
        fun value(): String
    }

    interface Parent {
        fun child(): Dependency
    }

    class LazySpringBeanWhichHoldsReferenceForBeanFactory(
        val beanFactory: ByteArray,
    ) {
        fun doSmth(): String = "Smth"
    }
}
