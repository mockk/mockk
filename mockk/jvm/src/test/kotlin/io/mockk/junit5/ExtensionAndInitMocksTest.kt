@file:Suppress("UNUSED_PARAMETER")

package io.mockk.junit5

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.AdditionalInterface
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class ExtensionAndInitMocksTest {
    @MockK
    private lateinit var car: Car

    @RelaxedMockK
    private lateinit var relaxedCar: Car

    @SpyK
    private var carSpy = Car()

    @MockK
    @AdditionalInterface(Runnable::class)
    private lateinit var runnableCar: Car

    @RelaxedMockK
    @AdditionalInterface(Runnable::class)
    private lateinit var runnableRelaxedCar: Car

    @SpyK
    @AdditionalInterface(Runnable::class)
    private var runnableCarSpy = Car()

    @Test
    fun initMocksAfterExtension() {
        val carSaved = car
        val relaxedCarSaved = relaxedCar
        val carSpySaved = carSpy

        MockKAnnotations.init(this)

        assertSame(carSaved, car)
        assertSame(relaxedCarSaved, relaxedCar)
        assertSame(carSpySaved, carSpy)

        MockKAnnotations.init(this)

        assertSame(carSaved, car)
        assertSame(relaxedCarSaved, relaxedCar)
        assertSame(carSpySaved, carSpy)
    }

    @Test
    fun allRunnable() {
        assertTrue { runnableCar as Any is Runnable }
        assertTrue { runnableRelaxedCar as Any is Runnable }
        assertTrue { runnableCarSpy as Any is Runnable }
    }
}
