package io.mockk.it

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CoroutineTest {

    data class ClearMocksClass(val a: String) {
        suspend fun a() {
            // logic
        }
    }
    /**
     * github issue #234
     */
    @Test
    fun clearMocksTest() {
        mockkConstructor(ClearMocksClass::class)
        coEvery { anyConstructed<ClearMocksClass>().a() } just runs
    }


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