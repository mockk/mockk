package io.mockk.gh

import io.mockk.*
import kotlin.test.*

class Issue35Test {
    class CloudBlockBlob {
        var metadata: HashMap<String?, String?>? = null
    }

    @Test
    fun stackOverflowInHashMap() {
        val blob: CloudBlockBlob = mockk()
        val metadata: HashMap<String?, String?> = hashMapOf()
        every { blob.metadata } returns metadata
    }

    @Test
    fun hashmapMock() {
        val map: HashMap<String, String> = mockk()
        every { map["abc"] } returns "def"
        every { map.put("ghi", "klm") } returns "nop"

        assertFailsWith<MockKException> { map["def"] }
        assertEquals("def", map["abc"])
        assertFailsWith<MockKException> { map.put("gh", "kl") }
        assertEquals("nop", map.put("ghi", "klm"));

//        verify(exactly = 0) { map["def"] }
        verify { map["abc"] }
//        verify(exactly = 0) { map.put("gh", "kl") }
        verify { map.put("ghi", "klm") }
    }
}
