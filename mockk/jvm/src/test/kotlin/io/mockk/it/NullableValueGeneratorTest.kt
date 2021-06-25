package io.mockk.it

import io.mockk.impl.JvmMockKGateway
import io.mockk.impl.instantiation.JvmAnyValueGenerator
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UNUSED_PARAMETER")
class NullableValueGeneratorTest {
    class NullableValueGenerator(
        voidInstance: Any
    ) : JvmAnyValueGenerator(voidInstance) {
        override fun anyValue(cls: KClass<*>, isNullable: Boolean, orInstantiateVia: () -> Any?): Any? {
            if (isNullable) return null
            return super.anyValue(cls, isNullable, orInstantiateVia)
        }
    }

    @Test
    fun testRelaxedMockReturnsNull() {
        JvmMockKGateway.anyValueGeneratorFactory = { voidInstance ->
            NullableValueGenerator(voidInstance)
        }

        class Bar

        @Suppress("RedundantNullableReturnType", "RedundantSuspendModifier")
        class Foo {
            val property: Bar? = Bar()
            val isEnabled: Boolean? = false
            fun getSomething(): Bar? = Bar()
            suspend fun getOtherThing(): Bar? = Bar()
        }

        val mock = mockk<Foo>(relaxed = true)
        assertEquals(null, mock.property)
        assertEquals(null, mock.isEnabled)
        assertEquals(null, mock.getSomething())
        assertEquals(null, runBlocking { mock.getOtherThing() })

        JvmMockKGateway.anyValueGeneratorFactory = { voidInstance ->
            JvmAnyValueGenerator(voidInstance)
        }
    }
}
