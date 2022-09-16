package io.mockk.core

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test


class ValueClassSupportTest {

    /** https://github.com/mockk/mockk/issues/868 */
    @Test
    fun `verify Java class does not cause KotlinReflectionInternalError`() {
        val mock = mockk<MockTarget> {
            every { func() } returns JavaEnum.A
        }

        mock.func() // throws KotlinReflectionInternalError
    }

}

private class MockTarget {
    fun func(): JavaEnum = JavaEnum.A
}
