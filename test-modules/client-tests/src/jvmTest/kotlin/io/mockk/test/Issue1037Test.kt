package io.mockk.test

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Issue1037Test {
    interface Api {
        fun foo() : Any
    }

    @Test
    fun `mock Any return type should not unbox Result class`() {
        val mockApi : Api = mockk()
        every { mockApi.foo() } returns Result.success(1)
        assertEquals(Result.success(1), mockApi.foo())
    }
}