package io.mockk.test

import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinVersionOverrideTest {

    @Test @kotlin.test.Ignore("2.0.0-Beta3 is reported as 2.0.0")
    fun `ensure Kotlin version is correctly handled`() = assertEquals(
        expected = System.getenv("kotlin.version").orEmpty(),
        actual = KotlinVersion.CURRENT.toString(),
    )

}
