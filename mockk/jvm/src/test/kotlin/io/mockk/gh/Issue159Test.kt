package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class Issue159Test {
    @Test
    fun testConcurrentGetMany() {
        val numValues = 10000
        val nThreads = 10
        val fetchTimes = numValues / nThreads

        class ClassToMock {
            fun getValue(): Int {
                return 0
            }
        }

        val mock = mockk<ClassToMock>(relaxed = true)
        val values = (1..numValues).toList()
        val sum = AtomicInteger()

        every { mock.getValue() } returnsMany values

        val latch = CountDownLatch(nThreads)
        (1..nThreads).map {
            thread {
                latch.countDown()
                latch.await()

                repeat(fetchTimes) {
                    sum.addAndGet(mock.getValue())
                }
            }
        }.forEach {
            it.join()
        }

        // expect to recive each value once
        assertEquals(values.sum(), sum.get())
    }
}