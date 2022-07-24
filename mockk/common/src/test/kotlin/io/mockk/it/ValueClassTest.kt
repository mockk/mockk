package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.jvm.JvmInline
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueClassTest {

    private val dummyValueWrapperArg get() = DummyValueWrapper(DummyValue(42))
    private val dummyValueWrapperReturn get() = DummyValueWrapper(DummyValue(99))

    private val dummyValueInnerArg get() = DummyValue(101)
    private val dummyValueInnerReturn get() = DummyValue(202)

    //<editor-fold desc="arg=Value Class, return=Wrapper">
    @Test
    fun `arg is ValueClass, returns Wrapper`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(dummyValueInnerArg) } returns dummyValueWrapperReturn
        }

        assertEquals(dummyValueWrapperReturn, mock.argValueClassReturnWrapper(dummyValueInnerArg))

        verify { mock.argValueClassReturnWrapper(dummyValueInnerArg) }
    }

    @Test
    fun `arg is any(ValueClass), returns Wrapper`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(any()) } returns dummyValueWrapperReturn
        }

        assertEquals(dummyValueWrapperArg, mock.argValueClassReturnWrapper(dummyValueInnerArg))

        verify { mock.argValueClassReturnWrapper(dummyValueInnerArg) }
    }

    @Test
    fun `arg is slot(ValueClass), returns Wrapper`() {
        val slot = slot<DummyValue>()

        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(capture(slot)) } returns dummyValueWrapperReturn
        }

        val result = mock.argValueClassReturnWrapper(dummyValueInnerArg)

        assertEquals(dummyValueWrapperReturn, result)

        assertEquals(dummyValueInnerArg, slot.captured)

        verify { mock.argValueClassReturnWrapper(dummyValueInnerArg) }
    }

    @Test
    fun `arg is ValueClass, answers Wrapper`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(dummyValueInnerArg) } answers { dummyValueWrapperReturn }
        }

        assertEquals(dummyValueWrapperReturn, mock.argValueClassReturnWrapper(dummyValueInnerArg))

        verify { mock.argValueClassReturnWrapper(dummyValueInnerArg) }
    }

    @Test
    fun `arg is any(ValueClass), answers Wrapper`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(any()) } answers { dummyValueWrapperReturn }
        }

        assertEquals(dummyValueWrapperReturn, mock.argValueClassReturnWrapper(dummyValueInnerArg))

        verify { mock.argValueClassReturnWrapper(dummyValueInnerArg) }
    }

    @Test
    fun `arg is slot(ValueClass), answers Wrapper`() {
        val slot = slot<DummyValue>()

        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(capture(slot)) } answers { dummyValueWrapperReturn }
        }

        val result = mock.argValueClassReturnWrapper(dummyValueInnerArg)

        assertEquals(dummyValueWrapperReturn, result)

        assertEquals(dummyValueInnerArg, slot.captured)

        verify { mock.argValueClassReturnWrapper(dummyValueInnerArg) }
    }
    //</editor-fold>

    //<editor-fold desc="arg=Value Class, return=ValueClass">
    @Test
    fun `arg is ValueClass, returns Inner`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(dummyValueInnerArg) } returns dummyValueInnerReturn
        }

        assertEquals(dummyValueInnerReturn, mock.argValueClassReturnValueClass(dummyValueInnerArg))

        verify { mock.argValueClassReturnValueClass(dummyValueInnerArg) }
    }

    @Test
    fun `arg is any(ValueClass), returns Inner`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(any()) } returns dummyValueInnerReturn
        }

        assertEquals(dummyValueInnerReturn, mock.argValueClassReturnValueClass(dummyValueInnerArg))

        verify { mock.argValueClassReturnValueClass(dummyValueInnerArg) }
    }

    @Test
    fun `arg is slot(ValueClass), returns Inner`() {
        val slot = slot<DummyValue>()
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(capture(slot)) } returns dummyValueInnerReturn
        }

        val result = mock.argValueClassReturnValueClass(dummyValueInnerArg)

        assertEquals(dummyValueInnerReturn, result)

        assertEquals(dummyValueInnerArg, slot.captured)

        verify { mock.argValueClassReturnValueClass(dummyValueInnerArg) }
    }

    @Test
    fun `arg is ValueClass, answers Inner`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(dummyValueInnerArg) } answers { dummyValueInnerReturn }
        }

        assertEquals(dummyValueInnerReturn, mock.argValueClassReturnValueClass(dummyValueInnerArg))

        verify { mock.argValueClassReturnValueClass(dummyValueInnerArg) }
    }

    @Test
    fun `arg is any(ValueClass), answers Inner`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(any()) } answers { dummyValueInnerReturn }
        }

        assertEquals(dummyValueInnerReturn, mock.argValueClassReturnValueClass(dummyValueInnerArg))

        verify { mock.argValueClassReturnValueClass(dummyValueInnerArg) }
    }

    @Test
    fun `arg is slot(ValueClass), answers Inner`() {
        val slot = slot<DummyValue>()

        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(capture(slot)) } answers { dummyValueInnerReturn }
        }

        val result = mock.argValueClassReturnValueClass(dummyValueInnerArg)

        assertEquals(dummyValueInnerReturn, result)

        assertEquals(dummyValueInnerArg, slot.captured)

        verify { mock.argValueClassReturnValueClass(dummyValueInnerArg) }
    }
    //</editor-fold>

    //<editor-fold desc="arg=Wrapper, return=ValueClass">
    @Test
    fun `arg is Outer, returns Inner`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(dummyValueWrapperArg) } returns dummyValueInnerReturn
        }

        assertEquals(dummyValueInnerReturn, mock.argWrapperReturnValueClass(dummyValueWrapperArg))

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is any(Outer), returns Inner`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(any()) } returns dummyValueInnerReturn
        }

        assertEquals(dummyValueInnerReturn, mock.argWrapperReturnValueClass(dummyValueWrapperArg))

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is slot(Outer), returns Inner`() {
        val slot = slot<DummyValueWrapper>()
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(capture(slot)) } returns dummyValueInnerReturn
        }

        val result = mock.argWrapperReturnValueClass(dummyValueWrapperArg)

        assertEquals(dummyValueInnerReturn, result)

        assertEquals(dummyValueWrapperArg, slot.captured)

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is Outer, answers Inner`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(dummyValueWrapperArg) } answers { dummyValueInnerReturn }
        }

        assertEquals(dummyValueInnerReturn, mock.argWrapperReturnValueClass(dummyValueWrapperArg))

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is any(Outer), answers Inner`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(any()) } answers { dummyValueInnerReturn }
        }

        assertEquals(dummyValueInnerReturn, mock.argWrapperReturnValueClass(dummyValueWrapperArg))

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is slot(Outer), answers Inner`() {
        val slot = slot<DummyValueWrapper>()

        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(capture(slot)) } answers { dummyValueInnerReturn }
        }

        val result = mock.argWrapperReturnValueClass(dummyValueWrapperArg)

        assertEquals(dummyValueInnerReturn, result)

        assertEquals(dummyValueWrapperArg, slot.captured)

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }
    //</editor-fold>

    //<editor-fold desc="arg=Wrapper, return=Wrapper">
    @Test
    fun `arg is Outer, returns Wrapper`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(dummyValueWrapperArg) } returns dummyValueWrapperReturn
        }

        assertEquals(dummyValueWrapperReturn, mock.argWrapperReturnWrapper(dummyValueWrapperArg))

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is any(Outer), returns Wrapper`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(any()) } returns dummyValueWrapperReturn
        }

        assertEquals(dummyValueWrapperReturn, mock.argWrapperReturnWrapper(dummyValueWrapperArg))

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is slot(Outer), returns Wrapper`() {
        val slot = slot<DummyValueWrapper>()
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(capture(slot)) } returns dummyValueWrapperReturn
        }

        val result = mock.argWrapperReturnWrapper(dummyValueWrapperArg)

        assertEquals(dummyValueWrapperReturn, result)

        assertEquals(dummyValueWrapperArg, slot.captured)

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is Outer, answers Wrapper`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(dummyValueWrapperArg) } answers { dummyValueWrapperReturn }
        }

        assertEquals(dummyValueWrapperReturn, mock.argWrapperReturnWrapper(dummyValueWrapperArg))

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is any(Outer), answers Wrapper`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(any()) } answers { dummyValueWrapperReturn }
        }

        assertEquals(dummyValueWrapperReturn, mock.argWrapperReturnWrapper(dummyValueWrapperArg))

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is slot(Outer), answers Wrapper`() {
        val slot = slot<DummyValueWrapper>()

        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(capture(slot)) } answers { dummyValueWrapperReturn }
        }

        val result = mock.argWrapperReturnWrapper(dummyValueWrapperArg)

        assertEquals(dummyValueWrapperReturn, result)

        assertEquals(dummyValueWrapperArg, slot.captured)

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }
    //</editor-fold>

    //<editor-fold desc="arg=None, return=UInt">
    /** https://github.com/mockk/mockk/issues/729 */
    @Test
    fun `arg None, returns UInt`() {
        val mock = mockk<DummyService> {
            every { argNoneReturnsUInt() } returns 999u
        }

        val result = mock.argNoneReturnsUInt()

        assertEquals(999u, result)
    }

    /** https://github.com/mockk/mockk/issues/729 */
    @Test
    fun `arg None, answers UInt`() {
        val mock = mockk<DummyService> {
            every { argNoneReturnsUInt() } answers { 999u }
        }

        val result = mock.argNoneReturnsUInt()

        assertEquals(999u, result)
    }
    //</editor-fold>

    //<editor-fold desc="extension functions">
    //<editor-fold desc="extension function on String">
    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is String, return is Inner`() {
        val fn = mockk<String.() -> DummyValue>()

        every { "string".fn() } returns dummyValueInnerReturn

        val result = "string".fn()

        assertEquals(dummyValueInnerReturn, result)
    }

    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is String, return is Wrapper`() {
        val fn = mockk<String.() -> DummyValueWrapper>()

        every { "string".fn() } returns dummyValueWrapperArg

        val result = "string".fn()

        assertEquals(dummyValueWrapperArg, result)
    }
    //</editor-fold>

    //<editor-fold desc="extension function on Outer">
    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is Outer, return is Wrapper`() {
        val fn = mockk<DummyValueWrapper.() -> DummyValueWrapper>()

        every { dummyValueWrapperArg.fn() } returns dummyValueWrapperArg

        val result = dummyValueWrapperArg.fn()

        assertEquals(dummyValueWrapperArg, result)
    }

    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is Outer, return is Inner`() {
        val fn = mockk<DummyValueWrapper.() -> DummyValue>()

        every { dummyValueWrapperArg.fn() } returns dummyValueInnerReturn

        val result = dummyValueWrapperArg.fn()

        assertEquals(dummyValueInnerArg, result)
    }

    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is Outer, return is String`() {
        val fn = mockk<DummyValueWrapper.() -> String>()

        every { dummyValueWrapperArg.fn() } returns "example"

        val result = dummyValueWrapperArg.fn()

        assertEquals("example", result)
    }
    //</editor-fold>

    //<editor-fold desc="extension function on Inner">
    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is Inner, return is Wrapper`() {
        val fn = mockk<DummyValue.() -> DummyValueWrapper>()

        every { dummyValueInnerArg.fn() } returns dummyValueWrapperReturn

        val result = dummyValueInnerArg.fn()

        assertEquals(dummyValueWrapperArg, result)
    }

    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is Inner, return is Inner`() {
        val fn = mockk<DummyValue.() -> DummyValue>()

        every { dummyValueInnerArg.fn() } returns dummyValueInnerReturn

        val result = dummyValueInnerArg.fn()

        assertEquals(dummyValueInnerReturn, result)
    }

    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is Inner, return is String`() {
        val fn = mockk<DummyValue.() -> String>()

        every { dummyValueInnerArg.fn() } returns "example"

        val result = dummyValueInnerArg.fn()

        assertEquals("example", result)
    }
    //</editor-fold>
    //</editor-fold>


    companion object {

        @JvmInline
        value class DummyValue(val value: Int)

        @JvmInline
        value class DummyValueWrapper(val value: DummyValue)

        class DummyService {

            fun argWrapperReturnWrapper(outer: DummyValueWrapper): DummyValueWrapper =
                DummyValueWrapper(DummyValue(0))

            fun argWrapperReturnValueClass(outer: DummyValueWrapper): DummyValue =
                DummyValue(0)

            fun argValueClassReturnWrapper(inner: DummyValue): DummyValueWrapper =
                DummyValueWrapper(inner)

            fun argValueClassReturnValueClass(inner: DummyValue): DummyValue =
                DummyValue(0)


            fun argNoneReturnsUInt(): UInt = 123u
        }
    }
}
