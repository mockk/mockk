package io.mockk.it

import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Issue #1288: calls made from field initializers and the Kotlin `init` block.
 *
 * `spyk(TestImpl(), recordPrivateCalls = true)` (copying an existing instance) can never see
 * these calls: the `init` block ran while the original object was constructed, before the spy
 * existed, so there is nothing to intercept. `spyk<TestImpl>()` on the other hand builds its
 * proxy by running the class's real default constructor, and since the init-block calls now
 * reach the spy they are recorded like any other call.
 */
class PrivateInitCallsTest {
    class TestImpl {
        var helloCalls = 0

        init {
            hello()
            greet()
        }

        fun initTest() {
            hello()
        }

        private fun hello() {
            helloCalls++
        }

        fun greet() {}
    }

    @Test
    fun sanityPrivateCallAfterSpyCreation() {
        val spy = spyk(TestImpl(), recordPrivateCalls = true)
        spy.initTest()
        verify(exactly = 1) { spy["hello"]() }
        assertEquals(2, spy.helloCalls)
    }

    @Test
    fun classSpykRecordsInitBlockPrivateCall() {
        val spy = spyk<TestImpl>(recordPrivateCalls = true)
        verify(exactly = 1) { spy["hello"]() }
    }

    @Test
    fun classSpykRecordsInitBlockPublicCall() {
        val spy = spyk<TestImpl>(recordPrivateCalls = true)
        verify(exactly = 1) { spy.greet() }
    }
}
