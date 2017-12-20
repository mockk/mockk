package io.mockk.impl.recording

import kotlin.test.Test
import kotlin.test.assertEquals

class ChildHinterTest {
    val hinter = ChildHinter()

    @Test
    fun givenNoHintsWhenNextChildTypeCalledThenDefaultChildTypeReturned() {
        val type = hinter.nextChildType { Double::class }
        assertEquals(Double::class, type)
    }

    @Test
    fun givenOneHintWhenNextChildTypeCalledThenItIsReturned() {
        hinter.hint(1, Int::class)
        val type = hinter.nextChildType { Double::class }
        assertEquals(Int::class, type)
    }

    @Test
    fun givenOneHintSecondPositionWhenNextChildTypeSecondTimeCalledThenItIsReturned() {
        hinter.hint(2, Int::class)
        assertEquals(Double::class, hinter.nextChildType { Double::class })
        assertEquals(Int::class, hinter.nextChildType { Double::class })
    }

    @Test
    fun givenTwoHintsWhenNextChildTypeCalledThenTheyAreReturned() {
        hinter.hint(1, Int::class)
        hinter.hint(2, String::class)
        assertEquals(Int::class, hinter.nextChildType { Double::class })
        assertEquals(String::class, hinter.nextChildType { Double::class })
    }
}