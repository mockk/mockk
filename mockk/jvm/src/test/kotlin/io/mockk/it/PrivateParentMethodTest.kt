package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test mocking non-visible parent methods through dynamic calls. Issue #425
 */
class PrivateParentMethodTest {
    open class Parent {
        open fun call(): String = callPrivate()
        private fun callPrivate() = "Real"
    }

    open class Child: Parent()

    class ChildWithShadowedMethod: Parent() {
        override fun call(): String = callPrivate()
        fun callPrivate() = "Shadowed"
    }

    class GrandChild: Child()

    @Test
    fun testChildAlwaysMockedFirst() {
        val mock = mockk<ChildWithShadowedMethod> {
            every { call() } answers { callOriginal() }
            every { this@mockk["callPrivate"]() } returns "Mock"
        }

        assertEquals(mock.call(), "Mock")
    }

    @Test
    fun testPrivateCallMock() {
        val mock = mockk<Child> {
            every { call() } answers { callOriginal() }
            every { this@mockk["callPrivate"]() } returns "Mock"
        }

        assertEquals(mock.call(), "Mock")
    }

    @Test
    fun testPrivateCallMockForGrandChild() {
        val mock = mockk<GrandChild> {
            every { call() } answers { callOriginal() }
            every { this@mockk["callPrivate"]() } returns "Mock"
        }

        assertEquals(mock.call(), "Mock")
    }

    @Test
    fun testPrivateCallVerify() {
        val mock = spyk(Child(), recordPrivateCalls = true)

        mock.call()

        verify { mock["callPrivate"]() }
    }
}
