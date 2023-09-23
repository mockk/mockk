package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.mockk.impl.log.JvmLogging
import io.mockk.impl.log.Logger
import java.util.Collections.synchronizedList
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test related to GitHub issue #123
 */
class ParallelTest {
    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }

    data class Op(val mock: MockCls, val a: Int, val b: Int)

    @MockK
    lateinit var mock1: MockCls
    @MockK
    lateinit var mock2: MockCls
    @MockK
    lateinit var mock3: MockCls
    @MockK
    lateinit var mock4: MockCls
    @MockK
    lateinit var mock5: MockCls
    @MockK
    lateinit var mock6: MockCls

    init {
        MockKAnnotations.init(this)
    }

    @Test
    fun test() {
        every { mock1.op(any(), any()) } answers { firstArg<Int>() - secondArg<Int>() }
        every { mock2.op(any(), any()) } answers { firstArg<Int>() * secondArg<Int>() }
        every { mock3.op(any(), any()) } answers { firstArg<Int>() / secondArg<Int>() }
        every { mock4.op(any(), any()) } answers { firstArg<Int>() xor secondArg() }
        every { mock5.op(any(), any()) } answers { firstArg<Int>() and secondArg() }
        every { mock6.op(any(), any()) } answers { firstArg<Int>() or secondArg() }

        val rnd = Random()
        val threads = mutableListOf<Thread>()
        val exceptions = synchronizedList(mutableListOf<Exception>())

        repeat(30) { n ->
            Thread {
                val mocks = listOf(mock1, mock2, mock3, mock4, mock5, mock6)
                try {
                    val ops = mutableListOf<Op>()
                    repeat(500) {
                        val mock = mocks[rnd.nextInt(mocks.size)]
                        val a = rnd.nextInt(100) + 5
                        val b = rnd.nextInt(100) + 5
                        mock.op(a, b)
                        ops.add(Op(mock, a, b))
                    }
                    log.info { "Done operations for thread $n" }

                    repeat(500) {
                        val op = ops[rnd.nextInt(ops.size)]
                        verify { op.mock.op(op.a, op.b) }
                    }
                    log.info { "Done verification block for thread $n" }
                } catch (ex: Exception) {
                    log.warn(ex) { "Exception for thread $n" }
                    exceptions.add(ex)
                }
            }
                .apply { start() }
                .apply { threads.add(this) }
        }

        threads.forEach { it.join() }

        assertTrue(exceptions.isEmpty())
    }

    companion object {
        val log: Logger = JvmLogging.slf4jOrJulLogging()(ParallelTest::class)
    }
}
