package io.mockk.test

import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinVersionOverrideTest {

    @Test @kotlin.test.Ignore("1.9.20-RC is reported as 1.9.20")
    fun `ensure Kotlin version is correctly handled`() = assertEquals(
        expected = System.getenv("kotlin.version").orEmpty(),
        actual = KotlinVersion.CURRENT.toString(),
    )

}
