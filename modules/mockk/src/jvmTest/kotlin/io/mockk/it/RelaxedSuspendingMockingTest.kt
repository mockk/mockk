package io.mockk.it

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UNUSED_PARAMETER")
class RelaxedSuspendingMockingTest {
    @Suppress("RedundantSuspendModifier")
    class MockCls {
        suspend fun op(a: Int, b: Int) = a + b
        suspend fun opUnit(a: Int, b: Int) {}
        suspend fun complexOp(a: Int, b: Int): List<Int> = listOf(a, b)
    }

    @Test
    fun rurfRegularOperationOk() {
        val mock = mockk<MockCls>(relaxUnitFun = true) {
            coEvery { op(1, 2) } returns 4
            coEvery { opUnit(1, 2) } returns Unit
            coEvery { complexOp(1, 2) } returns listOf(4, 5)
        }

        assertEquals(4, runBlocking { mock.op(1, 2) })
        assertEquals(Unit, runBlocking { mock.opUnit(1, 2) })
        assertEquals(listOf(4, 5), runBlocking { mock.complexOp(1, 2) })
    }

    @Test
    fun rurfFullyRelaxedRegularOperationOk() {
        val mock = mockk<MockCls>(relaxed = true)

        assertEquals(0, runBlocking { mock.op(1, 2) })
        assertEquals(emptyList(), runBlocking { mock.complexOp(1, 2) })
    }


    @Test
    fun rurfFullyRelaxedRegularUnitOperationOk() {
        val mock = mockk<MockCls>(relaxed = true)

        assertEquals(Unit, runBlocking { mock.opUnit(1, 2) })
    }
}
