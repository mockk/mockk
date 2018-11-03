package io.mockk.gh

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue51Test {
    data class Person(var name: String)

    class Team {
        protected var person: Person = Person("Init")
            get() = Person("Ben")
            set(value) {
                field = value
            }

        protected fun fn(arg: Int): Int = arg + 5
        fun pubFn(arg: Int) = fn(arg)

        var memberName: String
            get() = person.name
            set(value) {
                person = Person(value)
            }

    }

    @Test
    fun testPrivateProperty() {
        val mock = spyk(Team(), recordPrivateCalls = true)

        every { mock getProperty "person" } returns Person("Big Ben")
        every { mock setProperty "person" value Person("test") } just Runs
        every { mock invoke "fn" withArguments listOf(5) } returns 3

        assertEquals("Big Ben", mock.memberName)
        assertEquals(3, mock.pubFn(5))

        mock.memberName = "test"
        assertEquals("Big Ben", mock.memberName)

        verify { mock getProperty "person" }
        verify { mock setProperty "person" value Person("test") }
        verify { mock invoke "fn" withArguments listOf(5) }
    }

}