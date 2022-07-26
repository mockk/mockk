package io.mockk.jvm

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.*
import io.mockk.verify
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.condition.DisabledForJreRange
import org.junit.jupiter.api.condition.JRE

class HashMapMockTest {

    @Test
    fun canMockAHashMap() {
        val map = mockk<HashMap<String, String>>()
        every { map.put(any(), any()) } returnsArgument 1

        val value = map.put("key", "value")
        assertEquals("value", value)

        verify {
            map["key"] = "value"
        }
    }

    @Test
//    @DisabledForJreRange(max = JRE.JAVA_17)
//    @Disabled("Does not work anymore with jdk 17+")
    fun canSpyAHashMap() {
        val map = spyk<HashMap<String, String>>()
        assertDoesNotThrow { map["key"] = "value"  }

        verify(exactly = 1) { map["key"] = "value" }
    }

    @Test
//    @DisabledForJreRange(max = JRE.JAVA_16)
//    @Disabled("Does not work anymore with jdk 16+")
    fun concurrentHashMap_shouldBeSpied_Successfully() {
        val map = spyk(ConcurrentHashMap<String, String>())
        assertDoesNotThrow { map.put("key", "value")  }

        verify(exactly = 1) { map["key"] = "value" }
    }

    @Test
    @Disabled(value = "mocking of abstractMap don't work")
    fun abstractMap_shouldBeMocked_SuccessFully() {
        val map = mockk<AbstractMap<String, String>>()
        assertDoesNotThrow { map["key"] }

        verify(exactly = 1) { map["key"] }
    }
}
