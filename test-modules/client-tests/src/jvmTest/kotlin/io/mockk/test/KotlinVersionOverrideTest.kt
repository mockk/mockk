package io.mockk.test

import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinVersionOverrideTest {

    @Test
    fun `ensure Kotlin version is correctly handled`() = assertEquals(
        expected = System.getenv("kotlin.version").orEmpty(),
        actual = KotlinVersion.CURRENT.toString(),
    )

}
