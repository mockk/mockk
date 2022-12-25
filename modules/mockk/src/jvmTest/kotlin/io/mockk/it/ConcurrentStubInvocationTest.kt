package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals

/**
 * Executing stubbed methods concurrently sometimes returns wrong value.
 * Verifies issue #159.
 */
class ConcurrentStubInvocationTest {
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
