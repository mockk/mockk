package io.mockk.core

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

@JvmInline
value class InnerClass(val innerValue: Long)

@JvmInline
value class OuterClass(val outerValue: InnerClass)

class ContainerOfInnerClass(val innerClass: InnerClass)

class ContainerOfOuterClass(val outerClass: OuterClass)

class DefaultValueInMockedValueClassTest {

    // https://github.com/mockk/mockk/issues/1330
    @Test
    fun `given nested value class when relaxed mocking is enabled then value successfully verified`() {
        // pass
        val containerOfInnerClass = mockk<ContainerOfInnerClass>(relaxed = true)
        assertEquals(0L, containerOfInnerClass.innerClass.innerValue)

        // pass
        val outerClass = mockk<OuterClass>(relaxed = true)
        assertEquals(0L, outerClass.outerValue.innerValue)

        // fails with class "InnerClass cannot be cast to class java.lang.Long" - fixed
        val mockedClass = mockk<ContainerOfOuterClass>(relaxed = true)
        assertEquals(0L, mockedClass.outerClass.outerValue.innerValue)
    }

    @Test
    fun `given nested value class when relaxed mocking is disabled then value successfully verified`() {
        val containerOfInnerClass = mockk<ContainerOfInnerClass> {
            every { innerClass } returns InnerClass(innerValue = 0L)
        }
        assertEquals(0L, containerOfInnerClass.innerClass.innerValue)

        val mockedClass = mockk<ContainerOfOuterClass> {
            every { outerClass } returns OuterClass(outerValue = InnerClass(innerValue = 0L))
        }
        assertEquals(0L, mockedClass.outerClass.outerValue.innerValue)
    }
}