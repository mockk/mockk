package io.mockk.it

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CoroutineTest {
    /**
     * github issue #288
     */
    @Test
    fun suspendFnMocking(): Unit {
        val call = mockk<suspend () -> Int>()
        coEvery { call() } returns 5
        runBlocking { assertEquals(5, call()) }
    }
}