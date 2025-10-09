package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue259Test {

    class TestCls {
        fun fn() = "abc"
    }

    @Test
    fun test() {
        val n = 10
        val cyclicBarrier = CyclicBarrier(n)
        val latch = CountDownLatch(n)
        val result = AtomicInteger()
        val errors = AtomicInteger()
        repeat(n) { i ->
            thread {
                try {
                    cyclicBarrier.await()
                    val mock = mockk<TestCls>()
                    every { mock.fn() } returns "def $i"
                    if (mock.fn() == "def $i") {
                        result.incrementAndGet()
                    }
                } catch (ex: Throwable) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        assertEquals(0, errors.get(), "error count")
        assertEquals(n, result.get(), "result count")
    }
}