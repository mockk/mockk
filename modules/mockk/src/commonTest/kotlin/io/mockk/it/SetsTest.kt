package io.mockk.it

import io.mockk.every
import io.mockk.isMockKMock
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [Set]s related tests.
 */
class SetsTest {

    /**
     * See issue #221
     */
    @Test
    fun returnsSetOfMocks() {
        val foo = mockk<Foo>()
        every { foo.getTasks() } returns setOf(mockk(), mockk())

        val tasks = foo.getTasks()

        assertEquals(2, tasks.size)
        val task1 = tasks.first()
        val task2 = tasks.drop(1).first()

        assertTrue(isMockKMock(task1))
        assertTrue(isMockKMock(task2))
    }

    interface Foo {
        fun getTasks(): Set<Task>
    }

    interface Task
}
