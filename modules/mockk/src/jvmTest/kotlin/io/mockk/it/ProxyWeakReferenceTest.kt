package io.mockk.it

import io.mockk.clearAllStubsFromMemory
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference
import kotlin.test.assertEquals

class ProxyWeakReferenceTest {
    @Test
    @Disabled("Leaks memory. Objects are not removed from GC. https://github.com/mockk/mockk/pull/1448 TBD: leak issue")
    fun test() {
        for (i in 0..1000) {
            val spyk = spyk(LazySpringBeanWhichHoldsReferenceForBeanFactory(ByteArray(10 * 1024 * 1024)))
            every { spyk.doSmth() } returns "Wow"
            spyk.doSmth()
            clearMocks(spyk)
        }
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
        repeat(50) {
            if (refs.all { it.get() == null }) return
            System.gc()
            Thread.sleep(20)
        }
        assertEquals(0, refs.count { it.get() != null }, "mocks that were never garbage collected")
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

    class LazySpringBeanWhichHoldsReferenceForBeanFactory(
        val beanFactory: ByteArray,
    ) {
        fun doSmth(): String = "Smth"
    }
}
