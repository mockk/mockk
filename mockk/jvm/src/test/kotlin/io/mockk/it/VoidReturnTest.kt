package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlin.test.Test

/**
 * Test related to github issue #69
 */
class VoidReturnTest {
    abstract class KafkaFuture<T> {
        abstract operator fun get(timeout: Long, unit: TimeUnit): T
    }

    @Test
    fun test() {
        val kafkaFuture: KafkaFuture<Void> = mockk()
        every { kafkaFuture.get(any(), any()) } returns mockk()
        kafkaFuture.get(10, TimeUnit.MILLISECONDS)
    }
}