package io.mockk.jvm

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.ConcurrentHashMap
import org.junit.jupiter.api.Test

class HashMapMockTest {
    @Test
    @Disabled("fails on jdk 16")
    fun concurrentHashMap_shouldBeSpied_Successfully() {
        val map = spyk(ConcurrentHashMap<String, String>())
        assertDoesNotThrow { map.put("key", "value")  }

        verify(exactly = 1) { map.put("key", "value") }
    }

    @Test
    @Disabled(value = "mocking of abstractMap don't work")
    fun abstractMap_shouldBeMocked_SuccessFully() {
        val map = mockk<AbstractMap<String, String>>()
        assertDoesNotThrow { map.get("key")  }

        verify(exactly = 1) { map.get("key") }
    }
}
