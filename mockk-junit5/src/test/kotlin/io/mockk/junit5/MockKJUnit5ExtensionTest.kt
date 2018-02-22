package io.mockk.junit5

import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

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

class Car {
    @Suppress("UNUSED_PARAMETER")
    fun recordTelemetry(speed: Int, direction: Direction, lat: Double, long: Double): Outcome {
        return Outcome.FAILURE
    }
}

@ExtendWith(MockKJUnit5Extension::class)
class MockKJUnit5ExtensionTest {
    @MockK
    private lateinit var car2: Car

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
}
