package io.mockk.it

import io.mockk.every
import io.mockk.spyk
import kotlin.test.*

class SpyTest {
    @Test
    fun propertyInitialized() {
        val executed = arrayOf(false, false, false, false)
        val spyObj = spyk(SpyTestCls(executed))

        assertNotNull(spyObj.someReference)
    }

    @Test
    fun simpleSuperCall() {
        val executed = arrayOf(false, false, false, false)
        val spyObj = spyk(SpyTestCls(executed))

        every {
            spyObj.doSomething()
        } answers {
            callOriginal()
        }

        spyObj.doSomething()

        assertTrue(executed[0])
        assertTrue(executed[1])
        assertFalse(executed[2])
        assertFalse(executed[3])
    }

    @Test
    fun overrideCallOriginal() {
        val executed = arrayOf(false, false, false, false)
        val spyObj = spyk(SpyTestCls(executed))

        every {
            spyObj.computeSomething(1)
        } returns null

        assertNull(spyObj.computeSomething(1))

        assertFalse(executed[0])
        assertFalse(executed[1])
        assertFalse(executed[2])
        assertFalse(executed[3])
    }

    @Test
    fun secondSimpleSuperCall() {
        val executed = arrayOf(false, false, false, false)
        val spyObj = spyk(SpyTestCls(executed))

        every {
            spyObj.computeSomething(2)
        } answers { callOriginal()?.plus(4) }

        assertEquals(11, spyObj.computeSomething(2))

        assertFalse(executed[0])
        assertFalse(executed[1])
        assertTrue(executed[2])
        assertTrue(executed[3])
    }

    @Test
    fun notMockedCall() {
        val executed = arrayOf(false, false, false, false)
        val spyObj = spyk(SpyTestCls(executed))

        assertEquals(8, spyObj.computeSomething(3))

        assertFalse(executed[0])
        assertFalse(executed[1])
        assertTrue(executed[2])
        assertTrue(executed[3])
    }

    open class BaseTestCls(val someReference: String, val executed: Array<Boolean>) {
        open fun doSomething() {
            executed[0] = true
        }

        open fun computeSomething(a: Int): Int? {
            executed[2] = true
            return 22
        }
    }


    class SpyTestCls(executed: Array<Boolean>) : BaseTestCls("A spy", executed) {
        override fun doSomething() {
            executed[1] = true
            super.doSomething()
        }

        override fun computeSomething(a: Int): Int? {
            executed[3] = true
            super.computeSomething(a)
            return 5 + a
        }
    }
}