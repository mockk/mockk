package io.mockk.gh

import io.mockk.*
import kotlin.test.Test

class Issue234Test {
    data class Example(val a: String) {
        suspend fun a() {
            // logic
        }
    }

    @Test
    fun test() {
        mockkStatic("java.lang.System")
        mockkConstructor(Example::class)
        coEvery { anyConstructed<Example>().a() } just runs
    }
}