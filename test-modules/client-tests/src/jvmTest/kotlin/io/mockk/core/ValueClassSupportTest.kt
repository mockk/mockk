package io.mockk.core

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
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
    @Test
    fun `verify regular class is not affected by value class logic`() {
        val testValue = RegularClass("regular")

        val mock = mockk<ValueClassService> {
            every { getRegularClass() } returns testValue
        }

        assertEquals(testValue, mock.getRegularClass())
    }

    @Test
    fun `verify value class`() {
        val testValue = ValueClass("direct")

        val mock = mockk<ValueClassService> {
            every { getValueClass() } returns testValue
        }

        assertEquals(testValue, mock.getValueClass())
    }

    @Test
    fun `verify nullable value class`() {
        val testValue = ValueClass("nullable")

        val mock = mockk<ValueClassService> {
            every { getNullableValueClass() } returns testValue
        }

        assertEquals(testValue, mock.getNullableValueClass())
    }

    @Test
    fun `verify null value for nullable value class`() {
        val mock = mockk<ValueClassService> {
            every { getNullableValueClass() } returns null
        }

        assertNull(mock.getNullableValueClass())
    }

    @Test
    fun `verify value class property`() {
        val testValue = ValueClass("property")

        val mock = mockk<ValueClassService> {
            every { valueClassProperty } returns testValue
        }

        assertEquals(testValue, mock.valueClassProperty)
    }

    @Test
    fun `verify nullable value class property`() {
        val testValue = ValueClass("property")

        val mock = mockk<ValueClassService> {
            every { nullableValueClassProperty } returns testValue
        }

        assertEquals(testValue, mock.nullableValueClassProperty)
    }

    @Test
    fun `verify null value for nullable value class property`() {
        val mock = mockk<ValueClassService> {
            every { nullableValueClassProperty } returns null
        }

        assertNull(mock.nullableValueClassProperty)
    }

    /** https://github.com/mockk/mockk/issues/1342 */
    @Test
    fun `verify value class returned as its interface type`() {
        val testValue = ValueClass("as interface type")

        val mock = mockk<ValueClassService> {
            every { getValueClassAsInterfaceType() } returns testValue
        }

        assertEquals(testValue, mock.getValueClassAsInterfaceType())
    }

    @Test
    fun `verify nullable value class returned as its interface type`() {
        val testValue = ValueClass("as interface type")

        val mock = mockk<ValueClassService> {
            every { getNullableValueClassAsInterfaceType() } returns testValue
        }

        assertEquals(testValue, mock.getNullableValueClassAsInterfaceType())
    }

    @Test
    fun `verify null value for nullable value class returned as its interface type`() {
        val mock = mockk<ValueClassService> {
            every { getNullableValueClassAsInterfaceType() } returns null
        }

        assertNull(mock.getNullableValueClassAsInterfaceType())
    }

    @Test
    fun `verify value class property of its interface type`() {
        val testValue = ValueClass("as interface type property")

        val mock = mockk<ValueClassService> {
            every { valueClassAsInterfaceTypeProperty } returns testValue
        }

        assertEquals(testValue, mock.valueClassAsInterfaceTypeProperty)
    }

    @Test
    fun `verify nullable value class property of its interface type`() {
        val testValue = ValueClass("as interface type property")

        val mock = mockk<ValueClassService> {
            every { nullableValueClassAsInterfaceTypeProperty } returns testValue
        }

        assertEquals(testValue, mock.nullableValueClassAsInterfaceTypeProperty)
    }

    @Test
    fun `verify null value for nullable value class property of its interface type`() {
        val mock = mockk<ValueClassService> {
            every { nullableValueClassAsInterfaceTypeProperty } returns null
        }

        assertNull(mock.nullableValueClassAsInterfaceTypeProperty)
    }

    @Test
    fun `verify value class with primitive underlying type`() {
        val testValue = PrimitiveValueClass(0)

        val mock = mockk<ValueClassService> {
            every { getPrimitiveValueClass() } returns testValue
        }

        assertEquals(testValue, mock.getPrimitiveValueClass())
    }

    @Test
    fun `verify nullable value class with primitive underlying type`() {
        val testValue = PrimitiveValueClass(0)

        val mock = mockk<ValueClassService> {
            every { getNullablePrimitiveValueClass() } returns testValue
        }

        assertEquals(testValue, mock.getNullablePrimitiveValueClass())
    }

    @Test
    fun `verify null value for nullable value class with primitive underlying type`() {
        val mock = mockk<ValueClassService> {
            every { getNullablePrimitiveValueClass() } returns null
        }

        assertNull(mock.getNullablePrimitiveValueClass())
    }

    @Test
    fun `verify suspend function returning a value class`() {
        val testValue = ValueClass("suspend")

        val mock = mockk<ValueClassService> {
            coEvery { getSuspendValueClass() } returns testValue
        }

        val result = runBlocking { mock.getSuspendValueClass() }

        assertEquals(testValue, result)
    }

    @Test
    fun `verify suspend function returning a nullable value class`() {
        val testValue = ValueClass("suspend")

        val mock = mockk<ValueClassService> {
            coEvery { getSuspendNullableValueClass() } returns testValue
        }

        val result = runBlocking { mock.getSuspendNullableValueClass() }

        assertEquals(testValue, result)
    }

    @Test
    fun `verify suspend function returning null value for a nullable value class`() {
        val mock = mockk<ValueClassService> {
            coEvery { getSuspendNullableValueClass() } returns null
        }

        val result = runBlocking { mock.getSuspendNullableValueClass() }

        assertNull(result)
    }

    @Test
    fun `verify suspend function returning a value class with primitive underlying type`() {
        val testValue = PrimitiveValueClass(0)

        val mock = mockk<ValueClassService> {
            coEvery { getSuspendPrimitiveValueClass() } returns testValue
        }

        val result = runBlocking { mock.getSuspendPrimitiveValueClass() }

        assertEquals(testValue, result)
    }

    @Test
    fun `verify nested value class`() {
        val testValue = NestedValueClass(ValueClass("nested"))

        val mock = mockk<ValueClassService> {
            every { getNestedValueClass() } returns testValue
        }

        assertEquals(testValue, mock.getNestedValueClass())
    }

    @Test
    fun `verify value class returned as Any`() {
        val testValue = ValueClass("any-type")

        val mock = mockk<ValueClassService> {
            every { getValueClassAsAny() } returns testValue
        }

        assertEquals(testValue, mock.getValueClassAsAny())
    }
}

private class MockTarget {
    fun func(): JavaEnum = JavaEnum.A
}

internal data class RegularClass(val s: String)

internal interface ValueClassSuperType

@JvmInline
internal value class ValueClass(val s: String) : ValueClassSuperType

@JvmInline
internal value class PrimitiveValueClass(val value: Int)

@JvmInline
internal value class NestedValueClass(val inner: ValueClass)

internal interface ValueClassService {
    fun getRegularClass(): RegularClass
    fun getValueClass(): ValueClass
    val valueClassProperty: ValueClass
    fun getNullableValueClass(): ValueClass?
    val nullableValueClassProperty: ValueClass?
    fun getValueClassAsInterfaceType(): ValueClassSuperType
    val valueClassAsInterfaceTypeProperty: ValueClassSuperType
    fun getNullableValueClassAsInterfaceType(): ValueClassSuperType?
    val nullableValueClassAsInterfaceTypeProperty: ValueClassSuperType?
    fun getPrimitiveValueClass(): PrimitiveValueClass
    fun getNullablePrimitiveValueClass(): PrimitiveValueClass?
    fun getNestedValueClass(): NestedValueClass
    fun getValueClassAsAny(): Any
    suspend fun getSuspendValueClass(): ValueClass
    suspend fun getSuspendNullableValueClass(): ValueClass?
    suspend fun getSuspendPrimitiveValueClass(): PrimitiveValueClass
}

private data class TestDataClass(val v: ValueClassSuperType)
