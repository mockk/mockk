package io.mockk.core

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ValueClassSupportTest {
    /** https://github.com/mockk/mockk/issues/868 */
    @Test
    fun `verify Java class does not cause KotlinReflectionInternalError`() {
        val mock =
            mockk<MockTarget> {
                every { func() } returns JavaEnum.A
            }

        val result = mock.func() // check this doesn't throw KotlinReflectionInternalError

        assertEquals(JavaEnum.A, result)
    }
    /** https://github.com/mockk/mockk/issues/1342 */
    @Test
    fun `verify value class parameter does not cause ClassCastException`() {

        val testValue = ValueClass("testing")

        val mock = mockk<TestDataClass> {
            every { v } returns testValue
        }

        assertEquals(testValue, mock.v)
    }
}

private class MockTarget {
    fun func(): JavaEnum = JavaEnum.A
}

private interface ValueClassSuperType

@JvmInline
private value class ValueClass(val s: String) : ValueClassSuperType

private data class TestDataClass(val v: ValueClassSuperType)
