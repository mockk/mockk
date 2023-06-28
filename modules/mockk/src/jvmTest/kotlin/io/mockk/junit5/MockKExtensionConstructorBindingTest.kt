package io.mockk.junit5

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtensionConstructorBindingTest.Status.BAD
import io.mockk.junit5.MockKExtensionConstructorBindingTest.Status.CRITICAL
import io.mockk.junit5.MockKExtensionConstructorBindingTest.Status.GOOD
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class MockKExtensionConstructorBindingTest(
    @MockK private val leftWing: LeftWing,
    @SpyK private val rightWing: RightWing,
    @RelaxedMockK private val enginePart: EnginePart,
    @SpyK private val engine: Engine,
    @InjectMockKs private val plane: Plane,
) {

    enum class Status {
        GOOD, BAD, CRITICAL
    }

    class LeftWing {
        fun status() = GOOD
        fun describe() = "Left wing"
    }

    class RightWing {
        fun status() = GOOD
        fun describe() = "Right wing"
    }

    data class Engine(private val enginePart: EnginePart) {
        fun status() = enginePart.status()
        fun describe() = "Engine" + (enginePart.model() ?: "")
    }

    class EnginePart {
        fun status() = GOOD
        fun model(): String? = null
    }

    class Plane(
        private val leftWing: LeftWing,
        private val rightWing: RightWing,
        private val engine: Engine,
    ) {
        fun describeComponents() = listOf(leftWing.describe(), rightWing.describe(), engine.describe()).joinToString(" ")
        fun state() = listOf(leftWing.status(), rightWing.status(), engine.status()).maxByOrNull { it.ordinal } ?: CRITICAL
    }

    @Test
    fun `injects mocks by constructor`() {
        every { leftWing.describe() } returns "LW"
        every { leftWing.status() } returns GOOD

        every { rightWing.describe() } returns "RW"

        every { enginePart.status() } returns BAD

        assertEquals(plane.state(), BAD)
        assertEquals(plane.describeComponents(), "LW RW Engine")
        verify { engine.describe() }
    }
}
