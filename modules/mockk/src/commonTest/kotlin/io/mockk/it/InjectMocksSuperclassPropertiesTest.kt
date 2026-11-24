package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `@InjectMockKs` fills in `lateinit var`s declared in every class of the target's hierarchy,
 * including private ones (issue #1214).
 */
class InjectMocksSuperclassPropertiesTest {
    class ServiceA {
        fun doIt() = "real A"
    }

    class ServiceB {
        fun doIt() = "real B"
    }

    abstract class Grandparent {
        private lateinit var serviceA: ServiceA

        fun callA() = serviceA.doIt()
    }

    abstract class Parent : Grandparent() {
        private lateinit var serviceB: ServiceB

        fun callB() = serviceB.doIt()
    }

    class Child : Parent()

    @MockK
    private lateinit var serviceA: ServiceA

    @MockK
    private lateinit var serviceB: ServiceB

    @InjectMockKs
    private lateinit var child: Child

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { serviceA.doIt() } returns "stubbed A"
        every { serviceB.doIt() } returns "stubbed B"
    }

    @Test
    fun injectsPrivateLateinitDeclaredInTopMostSuperclass() {
        assertEquals("stubbed A", child.callA())
    }

    @Test
    fun injectsPrivateLateinitDeclaredInIntermediateSuperclass() {
        assertEquals("stubbed B", child.callB())
    }
}
