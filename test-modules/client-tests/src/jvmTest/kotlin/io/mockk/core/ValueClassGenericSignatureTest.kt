package io.mockk.core

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

// https://github.com/mockk/mockk/issues/1360
// Kotlin compiles a function taking a @JvmInline value class whose underlying type is generic into
// a name-mangled method whose generic `Signature` attribute can carry a wildcard on a type variable
// that is not declared anywhere Byte Buddy can see. Reflecting over such a signature used to kill
// proxy creation for interfaces, and the inline redefinition of classes.
@JvmInline
value class GenericOutBox<out A>(
    private val items: List<A>,
) : List<A> by items

@JvmInline
value class GenericBox<T>(
    val items: List<T>,
)

interface GenericOutBoxInterface {
    fun useOutBox(something: GenericOutBox<String>): String
}

class GenericOutBoxClass {
    fun useOutBox(something: GenericOutBox<String>): String = "original " + something.first()
}

interface GenericBoxInterface {
    val box: GenericBox<String>
}

class ValueClassGenericSignatureTest {
    @Test
    fun `given interface method taking generic value class with out variance when interface is mocked then call is intercepted`() {
        val service =
            mockk<GenericOutBoxInterface> {
                every { useOutBox(any()) } returns "stubbed"
            }
        assertEquals("stubbed", service.useOutBox(GenericOutBox(listOf("a", "b"))))
    }

    @Test
    fun `given class method taking generic value class with out variance when class is mocked then call is intercepted`() {
        val service =
            mockk<GenericOutBoxClass> {
                every { useOutBox(any()) } returns "stubbed"
            }
        assertEquals("stubbed", service.useOutBox(GenericOutBox(listOf("a", "b"))))
    }

    @Test
    fun `given interface property of generic value class when interface is mocked then property is stubbed`() {
        val service =
            mockk<GenericBoxInterface> {
                every { box } returns GenericBox(listOf("a"))
            }
        assertEquals(listOf("a"), service.box.items)
    }
}
