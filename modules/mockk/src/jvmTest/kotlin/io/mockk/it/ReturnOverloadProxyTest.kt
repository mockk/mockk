package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Overloads that erase to the same JVM name and parameter types but differ in their return
 * descriptors are legal Kotlin when the source-level parameter types differ only in their type
 * arguments (#1093): `invoke(FeatureFlag<Boolean>): Boolean` and `invoke(FeatureFlag<String>):
 * String` both compile to `invoke(FeatureFlag)`, with the return type as the only difference.
 *
 * The subclass proxy is built from a method graph that merges methods by name and parameter
 * types, so the two abstract methods collapse into one node and the generated proxy implements
 * only one of them. Calling the other throws [AbstractMethodError]
 * ("Subclass does not define or inherit an implementation of the resolved method ...").
 */
class ReturnOverloadProxyTest {
    interface FeatureFlag<T>

    class BoolFlag : FeatureFlag<Boolean>

    class StringFlag : FeatureFlag<String>

    interface IsFeatureFlagEnabledUseCase {
        operator fun invoke(flag: FeatureFlag<Boolean>): Boolean

        operator fun invoke(flag: FeatureFlag<String>): String
    }

    interface SingleMethodUseCase {
        operator fun invoke(flag: FeatureFlag<Boolean>): Boolean
    }

    @Test
    fun bothReturnOverloadsAreStubbedAndCalled() {
        val boolFlag = BoolFlag()
        val stringFlag = StringFlag()
        val useCase = mockk<IsFeatureFlagEnabledUseCase>()

        every { useCase.invoke(boolFlag) } returns true
        every { useCase.invoke(stringFlag) } returns "enabled"

        assertEquals(true, useCase.invoke(boolFlag))
        assertEquals("enabled", useCase.invoke(stringFlag))

        verify(exactly = 1) { useCase.invoke(boolFlag) }
        verify(exactly = 1) { useCase.invoke(stringFlag) }
    }

    @Test
    fun relaxedMockAnswersBothReturnOverloads() {
        val boolFlag = BoolFlag()
        val stringFlag = StringFlag()
        val useCase = mockk<IsFeatureFlagEnabledUseCase>(relaxed = true)

        useCase(boolFlag)
        useCase(stringFlag)

        verify(exactly = 1) { useCase.invoke(boolFlag) }
        verify(exactly = 1) { useCase.invoke(stringFlag) }
    }

    /**
     * Control: with the second overload removed there is no name+parameter collision and the
     * proxy implements the only remaining method - this passes on unpatched master as well.
     */
    @Test
    fun controlSingleMethodInterfaceIsMocked() {
        val boolFlag = BoolFlag()
        val useCase = mockk<SingleMethodUseCase>()

        every { useCase.invoke(boolFlag) } returns false

        assertEquals(false, useCase.invoke(boolFlag))

        verify { useCase.invoke(boolFlag) }
    }
}
