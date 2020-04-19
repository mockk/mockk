package io.mockk.gh

import io.mockk.*
import kotlin.test.Test

class Issue346Test {
    class Cls {
        private fun privateCall() = Unit

        fun pubCall() = privateCall()
    }

    @Test
    fun test() {
        val mock = spyk<Cls>(recordPrivateCalls = true)

        justRun { mock invokeNoArgs "privateCall" }

        mock.pubCall()
    }
}
