package io.mockk.gh

import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.verify
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class Issue121Test {
    @Test
    fun test() {
        mockkConstructor(File::class) {
            every {
                anyConstructed<File>().exists()
            } returns true
            assertTrue(File("x").exists())
        }
    }
}
