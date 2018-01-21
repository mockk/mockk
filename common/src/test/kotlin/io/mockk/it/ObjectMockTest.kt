package io.mockk.it

import io.mockk.every
import io.mockk.objectMockk
import io.mockk.use
import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectMockTest {
    object MockObj {
        fun add(a: Int, b: Int) = a + b
    }

    @Test
    fun objectMockByDefault() {
        objectMockk(MockObj).use {
            assertEquals(3, MockObj.add(1, 2))
        }
    }

    @Test
    fun objectMockWithDefinedBehaviour() {
        objectMockk(MockObj).use {
            every { MockObj.add(1, 2) } returns 55

            assertEquals(55, MockObj.add(1, 2))
        }
    }

}