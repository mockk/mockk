package io.mockk.gh

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.spyk
import kotlin.test.Test

class Issue346Test {
    class Cls {
        private fun privateCall() = Unit

        fun pubCall() = privateCall()
    }

    @Test
    fun test() {
        val mock = spyk<Cls>(recordPrivateCalls = true)

        every { mock invokeReturnsUnit "privateCall" } just Runs

        mock.pubCall()
    }
}
