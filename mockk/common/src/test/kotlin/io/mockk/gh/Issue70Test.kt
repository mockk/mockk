package io.mockk.gh

import io.mockk.*
import kotlin.test.Test

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