package io.mockk.gh

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import kotlin.test.Test

class Issue229Test {
    @Test
    fun test() {
        val taskCaptor = CapturingSlot<Callable<Boolean>>().apply {
            captured = Callable { true }
        }

        val asyncTaskExecutor = mockk<ExecutorService> {
            every { submit(capture(taskCaptor)) } answers {
                CompletableFuture.completedFuture(taskCaptor.captured.call())
            }
        }


        asyncTaskExecutor.submit(Callable<Boolean> { true })
    }
}