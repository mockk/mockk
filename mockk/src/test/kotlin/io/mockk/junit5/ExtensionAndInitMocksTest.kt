@file:Suppress("UNUSED_PARAMETER")
package io.mockk.junit5

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertSame

@ExtendWith(MockKExtension::class)
class ExtensionAndInitMocksTest {
    @MockK
    private lateinit var car: Car

    @RelaxedMockK
    private lateinit var relaxedCar: Car

    @SpyK
    private var carSpy = Car()

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
}
