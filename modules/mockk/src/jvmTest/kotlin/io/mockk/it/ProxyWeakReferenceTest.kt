package io.mockk.it

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ProxyWeakReferenceTest {
    @Disabled("Leaks memory. Objects are not removed from GC. https://github.com/mockk/mockk/pull/1448 TBD: leak issue")
    @Test
    fun test() {
        for (i in 0..1000) {
            val spyk = spyk(LazySpringBeanWhichHoldsReferenceForBeanFactory())
            every { spyk.doSmth() } returns "Wow"
            spyk.doSmth()
            clearMocks(spyk)
        }
    }

    class LazySpringBeanWhichHoldsReferenceForBeanFactory() {
        val dummyMemory = ByteArray(10 * 1024 * 1024)
        fun doSmth(): String {
            return "Smth"
        }
    }
}