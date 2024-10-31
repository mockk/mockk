package io.mockk.impl.jooq

import io.mockk.every
import io.mockk.mockk
import org.jooq.impl.TableImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JooqTest {

    @Test
    fun `should mock jOOQ TableImpl class`() {
        val tableImpl = mockk<TableImpl<*>>()
        every { tableImpl.name } returns "test name"
        assertEquals("test name", tableImpl.name)
    }

}
