package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test

/**
 * Test related to GitHub issue #69
 */
class VoidReturnTest {
    abstract class KafkaFuture<T> {
        abstract operator fun get(timeout: Long, unit: TimeUnit): T
    }

    @Test
    fun test() {
        val kafkaFuture: KafkaFuture<Void> = mockk()
        every { kafkaFuture[any(), any()] } returns mockk()
        kafkaFuture[10, TimeUnit.MILLISECONDS]
    }
}
