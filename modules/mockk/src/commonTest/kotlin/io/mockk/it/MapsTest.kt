package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Test class related to [Map]s mocking.
 */
class MapsTest {

    class CloudBlockBlob {
        var metadata: HashMap<String?, String?>? = null
    }

    /**
     * See issue #35
     */
    @Test
    fun stackOverflowInHashMap() {
        val blob: CloudBlockBlob = mockk()
        val metadata: HashMap<String?, String?> = hashMapOf()
        every { blob.metadata } returns metadata

        assertEquals(blob.metadata, metadata)
    }

    /**
     * See issue #35
     */
    @Test
    fun hashmapMock() {
        val map: HashMap<String, String> = mockk()
        every { map["abc"] } returns "def"
        every { map.put("ghi", "klm") } returns "nop"

        assertFailsWith<MockKException> { map["def"] }
        assertEquals("def", map["abc"])
        assertFailsWith<MockKException> { map["gh"] = "kl" }
        assertEquals("nop", map.put("ghi", "klm"))

        verify { map["abc"] }
        verify { map["ghi"] = "klm" }
        unmockkAll()
    }
}
