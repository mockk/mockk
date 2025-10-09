@file:Suppress("UNUSED_PARAMETER")
package io.mockk.junit5

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

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

class MockKExtensionTest {
    @MockK
    private lateinit var car2: Car

    @RelaxedMockK
    private lateinit var relaxedCar: Car

    @SpyK
    private var carSpy = Car()

    @Test
    fun injectsValidMockInMethods(@MockK car: Car) {
        every {
            car.recordTelemetry(
                    speed = more(50),
                    direction = Direction.NORTH,
                    lat = any(),
                    long = any()
            )
        } returns Outcome.RECORDED

        val result = car.recordTelemetry(51, Direction.NORTH, 1.0, 2.0)

        Assertions.assertEquals(Outcome.RECORDED, result)
    }

    @Test
    fun injectsValidMockInClass() {
        every {
            car2.recordTelemetry(
                    speed = more(50),
                    direction = Direction.NORTH,
                    lat = any(),
                    long = any()
            )
        } returns Outcome.RECORDED

        val result = car2.recordTelemetry(51, Direction.NORTH, 1.0, 2.0)

        Assertions.assertEquals(Outcome.RECORDED, result)
    }

    @Test
    fun injectsValidRelaxedMockInMethods(@RelaxedMockK car: Car) {
        val result = car.relaxedTest()

        Assertions.assertTrue(result is RelaxedOutcome)
    }

    @Test
    fun injectsValidRelaxedMockInClass() {
        val result = relaxedCar.relaxedTest()

        Assertions.assertTrue(result is RelaxedOutcome)
    }

    @Test
    fun testInjectsValidSpyInClass() {
        val result = carSpy.relaxedTest()

        Assertions.assertNull(result)

        verify { carSpy.relaxedTest() }
    }
}
