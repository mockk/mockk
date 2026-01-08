package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * See issue #47
 */
class InjectMocksTest {
    interface IFoo

    class Foo : IFoo {
        fun method() {
        }
    }

    abstract class AbstractBar<T : IFoo> {
        lateinit var foo: T
    }

    class Bar : AbstractBar<Foo>() {
        fun call() {
            foo.method()
        }
    }

    @MockK
    lateinit var foo: Foo

    @InjectMockKs
    lateinit var bar: Bar

    @BeforeTest
    fun setUp() = MockKAnnotations.init(this)

    @Test
    fun test() {
        every { foo.method() } answers { nothing }

        bar.call()

        verify { foo.method() }
    }
}

/**
 * See issue #1356
 * Test for injecting mocks as a List
 */
class InjectMocksListTest {
    interface IFoo {
        fun method()
    }

    class Foo1 : IFoo {
        override fun method() {}
    }

    class Foo2 : IFoo {
        override fun method() {}
    }

    class Bar(
        val foos: List<IFoo>,
    )

    @MockK
    lateinit var foo1: Foo1

    @MockK
    lateinit var foo2: Foo2

    @InjectMockKs
    lateinit var bar: Bar

    @BeforeTest
    fun setUp() = MockKAnnotations.init(this)

    @Test
    fun testListInjection() {
        kotlin.test.assertEquals(2, bar.foos.size)
        kotlin.test.assertTrue(bar.foos.contains(foo1))
        kotlin.test.assertTrue(bar.foos.contains(foo2))
    }
}
