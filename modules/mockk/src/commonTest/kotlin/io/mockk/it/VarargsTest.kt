package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@Suppress("UNUSED_PARAMETER")
class VarargsTest {
    val mock = mockk<VarargsCls>()

    @Test
    fun simpleArgs() {
        every { mock.intOp(5, 6, 7, c = 8) } returns 1
        assertEquals(1, mock.intOp(5, 6, 7, c = 8))
        verify { mock.intOp(5, 6, more(5), c = 8) }
    }

    @Test
    fun eqMatcher() {
        every { mock.intOp(6, eq(3), 7, c = 8) } returns 2
        assertEquals(2, mock.intOp(6, 3, 7, c = 8))
        verify { mock.intOp(6, any(), more(5), c = 8) }
    }

    @Test
    fun eqAnyMatchers() {
        every { mock.intOp(7, eq(3), any(), c = 8) } returns 3
        assertEquals(3, mock.intOp(7, 3, 22, c = 8))
        val slot = slot<Int>()
        verify { mock.intOp(7, capture(slot), more(20), c = 8) }
        assertEquals(3, slot.captured)
    }

    @Test
    fun anyVararg() {
        every {
            mock.intOp(
                7,
                5,
                *anyIntVararg(),
                4,
                c = 8
            )
        } returns 3

        assertEquals(
            3,
            mock.intOp(
                7,
                5,
                3,
                3,
                3,
                3,
                4,
                c = 8
            )
        )
        val slot = slot<Int>()
        verify {
            mock.intOp(
                7,
                5,
                *anyIntVararg(),
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun anyVarargWrapper() {
        every {
            mock.intWrapperOp(
                7,
                IntWrapper(5),
                *anyVararg(),
                IntWrapper(4),
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.intWrapperOp(
                7,
                IntWrapper(5),
                IntWrapper(3),
                IntWrapper(3),
                IntWrapper(3),
                IntWrapper(3),
                IntWrapper(4),
                c = 8
            )
        )
        val slot = slot<IntWrapper>()
        verify {
            mock.intWrapperOp(
                7,
                IntWrapper(5),
                *anyVararg(),
                capture(slot),
                c = 8
            )
        }
        assertEquals(IntWrapper(4), slot.captured)
    }


    @Test
    fun varargAnyIntWrapper() {
        every {
            mock.intWrapperOp(
                7,
                IntWrapper(5),
                *varargAny { it.value == 3 },
                IntWrapper(4),
                c = 8
            )
        } returns 3

        assertEquals(
            3, mock.intWrapperOp(
                7,
                IntWrapper(5),
                IntWrapper(3),
                IntWrapper(6),
                IntWrapper(2),
                IntWrapper(4),
                c = 8
            )
        )
        assertFails {
            mock.intOp(
                7,
                5,
                2,
                6,
                2,
                4,
                c = 8
            )
        }
        val slot = slot<IntWrapper>()
        verify {
            mock.intWrapperOp(
                7,
                IntWrapper(5),
                *varargAny { it.value == 3 },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured.value)
    }

    @Test
    fun varargAnyBoolean() {
        every {
            mock.booleanOp(
                7,
                true,
                *varargAnyBoolean { it },
                true,
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.booleanOp(
                7,
                true,
                false,
                true,
                false,
                true,
                c = 8
            )
        )
        val slot = slot<Boolean>()
        verify {
            mock.booleanOp(
                7,
                true,
                *varargAnyBoolean { it },
                capture(slot),
                c = 8
            )
        }
        assertEquals(true, slot.captured)
    }

    @Test
    fun varargAnyBooleanStart() {
        every {
            mock.booleanOp(
                7,
                *varargAnyBoolean { it },
                true,
                true,
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.booleanOp(
                7,
                true,
                false,
                true,
                true,
                true,
                c = 8
            )
        )
        val slot = slot<Boolean>()
        verify {
            mock.booleanOp(
                7,
                *varargAnyBoolean { it },
                true,
                capture(slot),
                c = 8
            )
        }
        assertEquals(true, slot.captured)
    }

    @Test
    fun varargAllBoolean() {
        every {
            mock.booleanOp(
                7,
                false,
                *varargAllBoolean { it },
                false,
                c = 8
            )
        } returns 3

        assertEquals(
            3, mock.booleanOp(
                7,
                false,
                true,
                true,
                true,
                false,
                c = 8
            )
        )
        assertFails {
            mock.booleanOp(
                7,
                false,
                false,
                false,
                false,
                false,
                c = 8
            )
        }
        val slot = slot<Boolean>()
        verify {
            mock.booleanOp(
                7,
                false,
                *varargAllBoolean { it },
                capture(slot),
                c = 8
            )
        }
        assertEquals(false, slot.captured)
    }

    @Test
    fun varargAnyByte() {
        every {
            mock.byteOp(
                7,
                5,
                *varargAnyByte { it == 3.toByte() },
                4,
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.byteOp(
                7,
                5,
                3,
                6,
                2,
                4,
                c = 8
            )
        )
        val slot = slot<Byte>()
        verify {
            mock.byteOp(
                7,
                5,
                *varargAnyByte { it == 3.toByte() },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun varargAllByte() {
        every {
            mock.byteOp(
                7,
                5,
                *varargAllByte { it == 3.toByte() },
                4,
                c = 8
            )
        } returns 3

        assertEquals(
            3, mock.byteOp(
                7,
                5,
                3,
                3,
                3,
                4,
                c = 8
            )
        )
        assertFails {
            mock.byteOp(
                7,
                4,
                4,
                4,
                4,
                4,
                c = 8
            )
        }
        val slot = slot<Byte>()
        verify {
            mock.byteOp(
                7,
                5,
                *varargAllByte { it == 3.toByte() },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun varargAnyChar() {
        every {
            mock.charOp(
                7,
                5.toChar(),
                *varargAnyChar { it == 3.toChar() },
                4.toChar(),
                c = 8
            )
        } returns 3

        assertEquals(
            3, mock.charOp(
                7,
                5.toChar(),
                3.toChar(),
                6.toChar(),
                2.toChar(),
                4.toChar(),
                c = 8
            )
        )

        val slot = slot<Char>()
        verify {
            mock.charOp(
                7,
                5.toChar(),
                *varargAnyChar { it == 3.toChar() },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4.toChar(), slot.captured)
    }

    @Test
    fun varargAllChar() {
        every {
            mock.charOp(
                7,
                5.toChar(),
                *varargAllChar { it == 3.toChar() },
                4.toChar(),
                c = 8
            )
        } returns 3

        assertEquals(
            3,
            mock.charOp(
                7,
                5.toChar(),
                3.toChar(),
                3.toChar(),
                3.toChar(),
                4.toChar(),
                c = 8
            )
        )
        assertFails {
            mock.charOp(
                7,
                4.toChar(),
                4.toChar(),
                4.toChar(),
                4.toChar(),
                4.toChar(),
                c = 8
            )
        }
        val slot = slot<Char>()
        verify {
            mock.charOp(
                7,
                5.toChar(),
                *varargAllChar { it == 3.toChar() },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4.toChar(), slot.captured)
    }

    @Test
    fun varargAnyShort() {
        every {
            mock.shortOp(
                7,
                5,
                *varargAnyShort { it == 3.toShort() },
                4,
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.shortOp(
                7,
                5,
                3,
                6,
                2,
                4,
                c = 8
            )
        )
        val slot = slot<Short>()
        verify {
            mock.shortOp(
                7,
                5,
                *varargAnyShort { it == 3.toShort() },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun varargAllShort() {
        every {
            mock.shortOp(
                7,
                5,
                *varargAllShort { it == 3.toShort() },
                4,
                c = 8
            )
        } returns 3
        assertEquals(
            3,
            mock.shortOp(
                7,
                5,
                3,
                3,
                3,
                4,
                c = 8
            )
        )
        assertFails {
            mock.shortOp(
                7,
                4,
                4,
                4,
                4,
                4,
                c = 8
            )
        }
        val slot = slot<Short>()
        verify {
            mock.shortOp(
                7,
                5,
                *varargAllShort { it == 3.toShort() },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun varargAnyInt() {
        every {
            mock.intOp(
                7,
                5,
                *varargAnyInt { it == 3 },
                4,
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.intOp(
                7,
                5,
                3,
                6,
                2,
                4,
                c = 8
            )
        )
        val slot = slot<Int>()
        verify {
            mock.intOp(
                7,
                5,
                *varargAnyInt { it == 3 },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun varargAllInt() {
        every {
            mock.intOp(
                7,
                5,
                *varargAllInt { it == 3 },
                4,
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.intOp(
                7,
                5,
                3,
                3,
                3,
                4,
                c = 8
            )
        )
        assertFails {
            mock.intOp(
                7,
                4,
                4,
                4,
                4,
                4,
                c = 8
            )
        }
        val slot = slot<Int>()
        verify {
            mock.intOp(
                7,
                5,
                *varargAllInt { it == 3 },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun varargAnyLong() {
        every {
            mock.longOp(
                7,
                5,
                *varargAnyLong { it == 3L },
                4,
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.longOp(
                7,
                5,
                3,
                6,
                2,
                4,
                c = 8
            )
        )
        val slot = slot<Long>()
        verify {
            mock.longOp(
                7,
                5,
                *varargAnyLong { it == 3L },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun varargAllLong() {
        every {
            mock.longOp(
                7,
                5,
                *varargAllLong { it == 3L },
                4,
                c = 8
            )
        } returns 3
        assertEquals(
            3, mock.longOp(
                7,
                5,
                3,
                3,
                3,
                4,
                c = 8
            )
        )
        assertFails {
            mock.longOp(
                7,
                4,
                4,
                4,
                4,
                4,
                c = 8
            )
        }
        val slot = slot<Long>()
        verify {
            mock.longOp(
                7,
                5,
                *varargAllLong { it == 3L },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4, slot.captured)
    }

    @Test
    fun varargAnyFloat() {
        every {
            mock.floatOp(
                7,
                5.0f,
                *varargAnyFloat { it == 3.0f },
                4.0f,
                c = 8
            )
        } returns 3.0f

        assertEquals(
            3.0f,
            mock.floatOp(
                7,
                5.0f,
                3.0f,
                6.0f,
                2.0f,
                4.0f,
                c = 8
            )
        )
        val slot = slot<Float>()
        verify {
            mock.floatOp(
                7,
                5.0f,
                *varargAnyFloat { it == 3.0f },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4.0f, slot.captured)
    }

    @Test
    fun varargAllFloat() {
        every {
            mock.floatOp(
                7,
                5.0f,
                *varargAllFloat { it == 3.0f },
                4.0f,
                c = 8
            )
        } returns 3.0f
        assertEquals(
            3.0f,
            mock.floatOp(
                7,
                5.0f,
                3.0f,
                3.0f,
                3.0f,
                4.0f,
                c = 8
            )
        )
        assertFails {
            mock.floatOp(
                7,
                4.0f,
                4.0f,
                4.0f,
                4.0f,
                4.0f,
                c = 8
            )
        }
        val slot = slot<Float>()
        verify {
            mock.floatOp(
                7,
                5.0f,
                *varargAllFloat { it == 3.0f },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4.0f, slot.captured)
    }

    @Test
    fun varargAnyDouble() {
        every {
            mock.doubleOp(
                7,
                5.0,
                *varargAnyDouble { it == 3.0 },
                4.0,
                c = 8
            )
        } returns 3.0
        assertEquals(
            3.0,
            mock.doubleOp(
                7,
                5.0,
                3.0,
                6.0,
                2.0,
                4.0,
                c = 8
            )
        )
        val slot = slot<Double>()
        verify {
            mock.doubleOp(
                7,
                5.0,
                *varargAnyDouble { it == 3.0 },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4.0, slot.captured)
    }

    @Test
    fun varargAllDouble() {
        every {
            mock.doubleOp(
                7,
                5.0,
                *varargAllDouble { it == 3.0 },
                4.0,
                c = 8
            )
        } returns 3.0

        assertEquals(
            3.0,
            mock.doubleOp(
                7,
                5.0,
                3.0,
                3.0,
                3.0,
                4.0,
                c = 8
            )
        )
        assertFails {
            mock.doubleOp(
                7,
                4.0,
                4.0,
                4.0,
                4.0,
                4.0,
                c = 8
            )
        }
        val slot = slot<Double>()
        verify {
            mock.doubleOp(
                7,
                5.0,
                *varargAllDouble { it == 3.0 },
                capture(slot),
                c = 8
            )
        }
        assertEquals(4.0, slot.captured)
    }

    @Test
    fun emptyVararg() {
        every { mock.intWrapperOp(1, c = 2) } returns 3

        mock.intWrapperOp(1, c = 2)

        verify { mock.intWrapperOp(1, c = 2) }
    }

    @Test
    fun anyVarargNoPrefixPostfix() {
        every { mock.intWrapperOp(1, *anyVararg(), c = 5) } returns 3

        mock.intWrapperOp(
            1,
            IntWrapper(2),
            IntWrapper(3),
            IntWrapper(4),
            c = 5
        )

        val slot1 = CapturingSlot<IntWrapper>()
        val slot2 = CapturingSlot<IntWrapper>()

        verify {
            mock.intWrapperOp(
                1,
                *anyVararg(),
                c = 5
            )
        }
        verify {
            mock.intWrapperOp(
                1,
                capture(slot1),
                capture(slot2),
                *anyVararg(),
                c = 5
            )
        }

        assertEquals(IntWrapper(2), slot1.captured)
        assertEquals(IntWrapper(3), slot2.captured)
    }

    data class IntWrapper(val value: Int)

    class VarargsCls {
        fun intWrapperOp(a: Int, vararg b: IntWrapper, c: Int, d: Int = 6) = b.sumOf { it.value } + a
        fun booleanOp(a: Int, vararg b: Boolean, c: Int, d: Int = 6) = b.sumOf { if (it) 1L else 0L } + a
        fun byteOp(a: Int, vararg b: Byte, c: Int, d: Int = 6) = b.sum() + a
        fun charOp(a: Int, vararg b: Char, c: Int, d: Int = 6) = b.sumOf { it.code } + a
        fun shortOp(a: Int, vararg b: Short, c: Int, d: Int = 6) = b.sum() + a
        fun intOp(a: Int, vararg b: Int, c: Int, d: Int = 6) = b.sum() + a
        fun longOp(a: Int, vararg b: Long, c: Int, d: Int = 6) = b.sum() + a
        fun floatOp(a: Int, vararg b: Float, c: Int, d: Int = 6) = b.sum() + a
        fun doubleOp(a: Int, vararg b: Double, c: Int, d: Int = 6) = b.sum() + a
    }
}
