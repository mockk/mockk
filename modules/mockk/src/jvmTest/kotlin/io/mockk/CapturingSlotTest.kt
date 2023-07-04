package io.mockk

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CapturingSlotTest {

    @Test
    fun `capturing slot behave as expected`() {
        val slot = CapturingSlot<String?>()
        // init state - exception is thrown if value is not yet captured
        val initStateMessage = assertThrows<IllegalStateException> { slot.captured }.message
        assertEquals(initStateMessage, "Value not yet captured.")
        assertFalse(slot.isCaptured)
        assertFalse(slot.isNull)
        // capture non null value
        slot.captured = "value"
        assertEquals(slot.captured, "value")
        assertTrue(slot.isCaptured)
        assertFalse(slot.isNull)
        // clear captured
        slot.clear()
        val afterNonNullClearMessage = assertThrows<IllegalStateException> { slot.captured }.message
        assertEquals(afterNonNullClearMessage, "Value not yet captured.")
        assertFalse(slot.isCaptured)
        assertFalse(slot.isNull)
        // capture null value
        slot.captured = null
        assertEquals(slot.captured, null)
        assertTrue(slot.isCaptured)
        assertTrue(slot.isNull)
        //clear captured
        slot.clear()
        val afterNullableClearMessage = assertThrows<IllegalStateException> { slot.captured }.message
        assertEquals(afterNullableClearMessage, "Value not yet captured.")
        assertFalse(slot.isCaptured)
        assertFalse(slot.isNull)
    }
}
