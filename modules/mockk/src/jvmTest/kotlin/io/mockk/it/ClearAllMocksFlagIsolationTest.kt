package io.mockk.it

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

fun flagTestTopLevel() = 0

class ClearAllMocksFlagIsolationTest {
    class Target {
        fun value() = 0
    }

    object ObjectTarget {
        fun value() = 0
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `regularMocks flag only clears regular and spy mocks`() {
        val mock = mockk<Target>(relaxed = true)
        val spy = spyk(Target())
        val obj = ObjectTarget
        mockkObject(obj)

        every { mock.value() } returns 10
        every { spy.value() } returns 20
        every { obj.value() } returns 30

        clearAllMocks(regularMocks = true, objectMocks = false, staticMocks = false, constructorMocks = false)

        assertEquals(0, mock.value())
        assertEquals(0, spy.value())
        assertEquals(30, obj.value())
    }

    @Test
    fun `objectMocks flag only clears object mocks`() {
        val mock = mockk<Target>(relaxed = true)
        val obj = ObjectTarget
        mockkObject(obj)

        every { mock.value() } returns 10
        every { obj.value() } returns 30

        clearAllMocks(regularMocks = false, objectMocks = true, staticMocks = false, constructorMocks = false)

        assertEquals(10, mock.value())
        assertEquals(0, obj.value())
    }

    @Test
    fun `staticMocks flag only clears static mocks`() {
        val mock = mockk<Target>(relaxed = true)
        mockkStatic("io.mockk.it.ClearAllMocksFlagIsolationTestKt")

        every { mock.value() } returns 10
        every { flagTestTopLevel() } returns 99

        clearAllMocks(regularMocks = false, objectMocks = false, staticMocks = true, constructorMocks = false)

        assertEquals(10, mock.value())
        assertEquals(0, flagTestTopLevel())
    }

    @Test
    fun `constructorMocks flag only clears constructor mocks`() {
        val mock = mockk<Target>(relaxed = true)
        mockkConstructor(Target::class)

        every { mock.value() } returns 10
        every { anyConstructed<Target>().value() } returns 77

        clearAllMocks(regularMocks = false, objectMocks = false, staticMocks = false, constructorMocks = true)

        assertEquals(10, mock.value())
        assertEquals(0, Target().value())
    }

    @Test
    fun `no flags set clears nothing`() {
        val mock = mockk<Target>(relaxed = true)
        val obj = ObjectTarget
        mockkObject(obj)

        every { mock.value() } returns 10
        every { obj.value() } returns 30

        clearAllMocks(regularMocks = false, objectMocks = false, staticMocks = false, constructorMocks = false)

        assertEquals(10, mock.value())
        assertEquals(30, obj.value())
    }
}
