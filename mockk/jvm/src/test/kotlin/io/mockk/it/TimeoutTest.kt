package io.mockk.it

import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFails
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class TimeoutTest {
    class MockCls {
        fun run() {

        }
    }

    @RelaxedMockK
    lateinit var mock1: MockCls

    @RelaxedMockK
    lateinit var mock2: MockCls

    @Test
    fun timeoutOnNeverCalled() {
        assertFails {
            verify(timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun timeoutOnCalledLater() {
        launch {
            delay(1000)
            mock1.run()
        }

        assertFails {
            verify(timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun okIfCalledEarlier() {
        launch {
            delay(200)
            mock1.run()
        }

        verify(timeout = 500) {
            mock1.run()
        }
    }

    @Test
    fun failIfOneMockCalled() {
        launch {
            delay(200)
            mock1.run()
        }

        assertFails {
            verify(timeout = 500) {
                mock1.run()
                mock2.run()
            }
        }
    }

    @Test
    fun failIfOneMockCalledAndOneLater() {
        launch {
            delay(200)
            mock1.run()
            delay(1000)
            mock2.run()
        }

        assertFails {
            verify(timeout = 500) {
                mock1.run()
                mock2.run()
            }
        }
    }

    @Test
    fun okIfBothInTime() {
        launch {
            delay(100)
            mock1.run()
            delay(100)
            mock2.run()
        }

        verify(timeout = 500) {
            mock1.run()
            mock2.run()
        }
    }


    @Test
    fun longTimeout() {
        launch {
            delay(500)
            mock1.run()
        }

        val start = System.currentTimeMillis()
        verify(timeout = 5000) {
            mock1.run()
        }
        val end = System.currentTimeMillis()
        val duration = end - start

        assertTrue(duration < 1500, "waiting too long")
    }

    private fun launch(block: () -> Unit) {
        Thread(block).start()
    }

    private fun delay(ms: Long) = Thread.sleep(ms)
}