package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectMockTest {
    object MockObj {
        fun add(a: Int, b: Int) = a + b
    }

    @Test
    fun objectMockByDefault() {
        mockkObject(MockObj)

        assertEquals(3, MockObj.add(1, 2))
    }

    @Test
    fun objectMockWithDefinedBehaviour() {
        mockkObject(MockObj)

        every { MockObj.add(1, 2) } returns 55

        assertEquals(55, MockObj.add(1, 2))
    }

    @Test
    fun objectMockCleared() {
        mockkObject(MockObj)

        every { MockObj.add(1, 2) } returns 55

        assertEquals(55, MockObj.add(1, 2))

        verify { MockObj.add(1, 2) }

        mockkObject(MockObj)

        verify(exactly = 0) { MockObj.add(1, 2) }

        every { MockObj.add(1, 2) } returns 33

        assertEquals(33, MockObj.add(1, 2))

        verify { MockObj.add(1, 2) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun objectMockUnmockCanBeSeparate() {
        val x = MockCls()
        objectMockk(x).mock()
        objectMockk(x).unmock()
    }

}