package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class HierarchicalMockingTest {
    interface Goal {
        fun getRootTask(): Task
    }

    interface Task {
        fun getSubTask(): Task

        fun doIt(): Int
    }


    @Test
    fun hierarchicalMocking() {
        val foo = mockk<Goal> {
            every { getRootTask() } returns mockk {
                every { doIt() } returns 5
            } andThen mockk<Task> {
                every { doIt() } returns 6 andThen 7

                every { getSubTask() } returns mockk {
                    every { doIt() } returns 8 andThen 9
                }
            }
        }

        assertEquals(5, foo.getRootTask().doIt())
        assertEquals(6, foo.getRootTask().doIt())
        assertEquals(7, foo.getRootTask().doIt())
        assertEquals(8, foo.getRootTask().getSubTask().doIt())
        assertEquals(9, foo.getRootTask().getSubTask().doIt())
    }
}