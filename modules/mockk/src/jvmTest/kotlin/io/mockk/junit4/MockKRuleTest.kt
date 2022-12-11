@file:Suppress("UNUSED_PARAMETER")

package io.mockk.junit4

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockKRuleTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    enum class Direction {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    enum class Outcome {
        FAILURE,
        RECORDED
    }

    class RelaxedOutcome

    class Car {
        fun recordTelemetry(speed: Int, direction: Direction, lat: Double, long: Double): Outcome {
            return Outcome.FAILURE
        }

        fun relaxedTest(): RelaxedOutcome? {
            return null
        }
    }

    @MockK
    private lateinit var car: Car

    @RelaxedMockK
    private lateinit var relaxedCar: Car

    @SpyK
    private var carSpy = Car()

    @Test
    fun injectsValidMockInClass() {
        every {
            car.recordTelemetry(
                speed = more(50),
                direction = Direction.NORTH,
                lat = any(),
                long = any()
            )
        } returns Outcome.RECORDED

        val result = car.recordTelemetry(51, Direction.NORTH, 1.0, 2.0)

        assertEquals(Outcome.RECORDED, result)
    }

    @Test
    fun injectsValidRelaxedMockInClass() {
        val result = relaxedCar.relaxedTest()

        assertTrue(result is RelaxedOutcome)
    }

    @Test
    fun testInjectsValidSpyInClass() {
        val result = carSpy.relaxedTest()

        assertNull(result)

        verify { carSpy.relaxedTest() }
    }
}
