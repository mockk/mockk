package io.mockk.gh

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.spyk
import kotlin.test.Test

@Suppress("UNUSED_PARAMETER")
class Issue70Test {
    class Cls {
        private fun <T> updateItemInDb(id: Long, column: String, data: T) {
        }

        fun pubCall() {
            updateItemInDb(1L, "abc", "data")
        }
    }

    @Test
    fun test() {
        val mock = spyk<Cls>();

        every {
            mock["updateItemInDb"](any<Long>(), any<String>(), any()) as Unit
        } just Runs

        mock.pubCall()
    }

}