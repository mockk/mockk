package io.mockk

import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses
import kotlin.test.Test
import kotlin.test.assertTrue

class InterfaceMockReflectionTest {
    interface SomeInterface

    class SomeImpl : SomeInterface

    @Test
    fun `mock of an interface should reflect as a subclass of that interface`() {
        val someMock = mockk<SomeInterface>()

        assertTrue(
            SomeInterface::class.java.isAssignableFrom(someMock::class.java),
            "java isAssignableFrom: mock class ${someMock::class.java.name}",
        )
        assertTrue(
            someMock::class.isSubclassOf(SomeInterface::class),
            "kotlin isSubclassOf: mock class ${someMock::class.qualifiedName}, superclasses=${someMock::class.superclasses}",
        )
    }

    @Test
    fun `mock of a subclass of an interface should reflect as a subclass of that interface`() {
        val anotherMock = mockk<SomeImpl>()

        assertTrue(
            anotherMock::class.isSubclassOf(SomeInterface::class),
            "kotlin isSubclassOf: mock class ${anotherMock::class.qualifiedName}",
        )
    }

    @Test
    fun `interface mock superclass list should contain the mocked interface`() {
        val someMock = mockk<SomeInterface>()

        assertTrue(
            someMock::class.superclasses.contains(SomeInterface::class),
            "superclasses=${someMock::class.superclasses.map { it.qualifiedName }}",
        )
    }
}
