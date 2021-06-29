package io.mockk.race

import io.mockk.every
import io.mockk.mockkStatic
import java.time.LocalDateTime
import kotlin.test.Test

/**
 * This test case is part A for the two-testcase test. This
 * test mocks the static type and do not unmock it.
 */
class StaticMockStateATest {

    @Test
    fun prepareTheScenario() {
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns LocalDateTime.parse("2020-01-01T12:00:00")
    }

}
