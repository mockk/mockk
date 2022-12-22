package io.mockk.core

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class ValueClassSupportTest {

    /** https://github.com/mockk/mockk/issues/868 */
    @Test
    fun `verify Java class does not cause KotlinReflectionInternalError`() {
        val mock = mockk<MockTarget> {
            every { func() } returns JavaEnum.A
        }

        val result = mock.func() // check this doesn't throw KotlinReflectionInternalError

        assertEquals(JavaEnum.A, result)
    }

}

private class MockTarget {
    fun func(): JavaEnum = JavaEnum.A
}
