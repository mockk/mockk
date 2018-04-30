package io.mockk

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class SimpleInstrumentationAndroidTest {

    class DexTest {
        fun abc(a: Int, b: Int) = a + b
    }


    @Test
    @Throws(Exception::class)
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

        assertEquals("io.mockk.test", appContext.packageName)

        val mock = mockk<DexTest>()

        every { mock.abc(2, 2) } returns 5

        println(mock.abc(2, 2))
    }
}
