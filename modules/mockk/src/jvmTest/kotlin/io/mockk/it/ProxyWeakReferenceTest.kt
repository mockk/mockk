package io.mockk.it

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test

class ProxyWeakReferenceTest {
    @Test
    fun test() {
        for (i in 0..1000) {
            val spyk = spyk(LazySpringBeanWhichHoldsReferenceForBeanFactory(ByteArray(10 * 1024 * 1024)))
            every { spyk.doSmth() } returns "Wow"
            spyk.doSmth()
            clearMocks(spyk)
        }
    }

    class LazySpringBeanWhichHoldsReferenceForBeanFactory(val beanFactory: ByteArray) {
        fun doSmth(): String {
            return "Smth"
        }
    }
}