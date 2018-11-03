package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class Issue69Test {
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