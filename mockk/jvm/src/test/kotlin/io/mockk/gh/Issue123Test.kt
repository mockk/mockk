package io.mockk.gh

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.*
import java.util.Collections.synchronizedList
import kotlin.test.Test
import kotlin.test.assertTrue

class Issue123Test {
    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }

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
                    data class Op(val mock: MockCls, val a: Int, val b: Int)

                    val ops = mutableListOf<Op>()
                    repeat(500) {
                        val mock = mocks[rnd.nextInt(mocks.size)]
                        val a = rnd.nextInt(100) + 5
                        val b = rnd.nextInt(100) + 5
                        mock.op(a, b)
                        ops.add(Op(mock, a, b))
                    }
                    log.info("Done operations for thread {}", n)

                    repeat(500) {
                        val op = ops[rnd.nextInt(ops.size)]
                        verify { op.mock.op(op.a, op.b) }
                    }
                    log.info("Done verification block for thread {}", n)
                } catch (ex: Exception) {
                    log.warn("Exception for thread {}", n, ex)
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
        val log = LoggerFactory.getLogger(Issue123Test::class.java)
    }
}