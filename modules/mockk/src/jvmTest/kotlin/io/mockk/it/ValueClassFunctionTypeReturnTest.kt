package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Value classes returned through function types and SAM interfaces.
 *
 * The synthetic `invoke` of such a type has no Kotlin descriptor kotlin-reflect can
 * map, so the unboxing decision for the return value has to degrade to passing the
 * boxed value through instead of crashing with a `KotlinReflectionInternalError`.
 *
 * https://github.com/mockk/mockk/issues/1242
 */
class ValueClassFunctionTypeReturnTest {
    @JvmInline
    value class SamValueClass(
        val value: Int,
    )

    interface SamValueClassInterface : () -> SamValueClass

    interface SamPlainClassInterface : () -> String

    @Test
    fun samInterfaceReturningValueClassIsStubbed() {
        val mock = mockk<SamValueClassInterface>()

        every { mock() } returns SamValueClass(1)

        assertEquals(1, mock().value)
        verify { mock() }
    }

    @Test
    fun samInterfaceReturningPlainClassIsStubbed() {
        val mock = mockk<SamPlainClassInterface>()

        every { mock() } returns "ok"

        assertEquals("ok", mock())
        verify { mock() }
    }

    @Test
    fun functionTypeReturningValueClassIsStubbed() {
        val mock = mockk<() -> SamValueClass>()

        every { mock() } returns SamValueClass(5)

        assertEquals(5, mock().value)
        verify { mock() }
    }

    @Test
    fun funInterfaceReturningValueClassIsStubbed() {
        val mock = mockk<FunInterfaceValueClassReturn>()

        every { mock.invoke() } returns SamValueClass(7)

        assertEquals(7, mock.invoke().value)
        verify { mock.invoke() }
    }

    fun interface FunInterfaceValueClassReturn {
        fun invoke(): SamValueClass
    }
}
