package io.mockk.test

import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinVersionOverrideTest {

    @Test
    fun `ensure Kotlin version is correctly handled`() = assertEquals(
        actual = System.getenv("kotlin.version").orEmpty(),
        expected = KotlinVersion.CURRENT.toString(),
    )

}