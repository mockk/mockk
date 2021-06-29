package io.mockk.race

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

object Obj {
    val DATE: LocalDateTime = LocalDateTime.parse("2000-05-01T10:10:00")
}

/**
 * This test case is part B for the two-testcase test. This
 * test uses LocalDateTime without any static mock.
 */
class StaticMockStateBTest {

    @Test
    fun mockWithoutStaticMock() {
        val mock = mockk<Foo> {
            every { doSomething(Obj.DATE) } returns 10
        }
        assertEquals(10, mock.doSomething(Obj.DATE))
    }

    class Foo {
        fun doSomething(ldt: LocalDateTime): Int = 0
    }
}
