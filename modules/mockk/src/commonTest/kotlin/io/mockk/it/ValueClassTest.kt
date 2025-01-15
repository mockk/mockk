package io.mockk.it

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueClassTest {

    private val dummyValueWrapperArg get() = DummyValueWrapper(DummyValue(42))
    private val dummyValueWrapperReturn get() = DummyValueWrapper(DummyValue(99))

    private val dummyValueClassArg get() = DummyValue(101)
    private val dummyComplexValueClassArg get() = ComplexValue(UUID.fromString("4d19b22c-7754-4c55-ba4d-f80109708a1f"))
    private val dummyValueClassReturn get() = DummyValue(202)
    private val dummyComplexValueClassReturn get() = ComplexValue(UUID.fromString("25581db2-4cdb-48cd-a6c9-e087aee31f0b"))

    //<editor-fold desc="arg=Value Class, return=ValueClass">
    @Test
    fun `arg is ValueClass, returns ValueClass`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(dummyValueClassArg) } returns dummyValueClassReturn
        }

        assertEquals(dummyValueClassReturn, mock.argValueClassReturnValueClass(dummyValueClassArg))

        verify { mock.argValueClassReturnValueClass(dummyValueClassArg) }
    }

    @Test
    fun `field is ValueClass, returns ValueClass`() {
        val mock = mockk<DummyService> {
            every { valueClassField } returns dummyValueClassReturn
        }

        assertEquals(dummyValueClassReturn, mock.valueClassField)

        verify { mock.valueClassField }
    }

    @Test
    fun `arg is any(ValueClass), returns ValueClass`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(any()) } returns dummyValueClassReturn
        }

        assertEquals(dummyValueClassReturn, mock.argValueClassReturnValueClass(dummyValueClassArg))

        verify { mock.argValueClassReturnValueClass(dummyValueClassArg) }
    }

    @Test
    fun `arg is slot(ValueClass), returns ValueClass`() {
        val slot = slot<DummyValue>()
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(capture(slot)) } returns dummyValueClassReturn
        }

        val result = mock.argValueClassReturnValueClass(dummyValueClassArg)

        assertEquals(dummyValueClassReturn, result)

        assertEquals(dummyValueClassArg, slot.captured)

        verify { mock.argValueClassReturnValueClass(dummyValueClassArg) }
    }

    @Test
    fun `arg is MutableList(ValueClass), returns ValueClass`() {
        val slot = mutableListOf<DummyValue>()
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(capture(slot)) } returns dummyValueClassReturn
        }

        val result = mock.argValueClassReturnValueClass(dummyValueClassArg)

        assertEquals(dummyValueClassReturn, result)

        assertEquals(dummyValueClassArg, slot.single())

        verify { mock.argValueClassReturnValueClass(dummyValueClassArg) }
    }

    @Test
    fun `arg is ValueClass, answers ValueClass`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(dummyValueClassArg) } answers { dummyValueClassReturn }
        }

        assertEquals(dummyValueClassReturn, mock.argValueClassReturnValueClass(dummyValueClassArg))

        verify { mock.argValueClassReturnValueClass(dummyValueClassArg) }
    }

    @Test
    fun `arg is any(ValueClass), answers ValueClass`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(any()) } answers { dummyValueClassReturn }
        }

        assertEquals(dummyValueClassReturn, mock.argValueClassReturnValueClass(dummyValueClassArg))

        verify { mock.argValueClassReturnValueClass(dummyValueClassArg) }
    }

    @Test
    fun `arg is slot(ValueClass), answers ValueClass`() {
        val slot = slot<DummyValue>()

        val mock = mockk<DummyService> {
            every { argValueClassReturnValueClass(capture(slot)) } answers { dummyValueClassReturn }
        }

        val result = mock.argValueClassReturnValueClass(dummyValueClassArg)

        assertEquals(dummyValueClassReturn, result)

        assertEquals(dummyValueClassArg, slot.captured)

        verify { mock.argValueClassReturnValueClass(dummyValueClassArg) }
    }
    //</editor-fold>

    //<editor-fold desc="arg=Complex Value Class, return=ComplexValueClass">
    @Test
    fun `arg is ComplexValueClass, returns ComplexValueClass`() {
        val mock = mockk<DummyService> {
            every { argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg) } returns dummyComplexValueClassReturn
        }

        assertEquals(dummyComplexValueClassReturn, mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg))

        verify { mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg) }
    }

    @Test
    fun `arg is any(ComplexValueClass), returns ComplexValueClass`() {
        val mock = mockk<DummyService> {
            every { argComplexValueClassReturnComplexValueClass(any()) } returns dummyComplexValueClassReturn
        }

        assertEquals(dummyComplexValueClassReturn, mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg))

        verify { mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg) }
    }

    @Test
    fun `arg is slot(ComplexValueClass), returns ComplexValueClass`() {
        val slot = slot<ComplexValue>()
        val mock = mockk<DummyService> {
            every { argComplexValueClassReturnComplexValueClass(capture(slot)) } returns dummyComplexValueClassReturn
        }

        val result = mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg)

        assertEquals(dummyComplexValueClassReturn, result)

        assertEquals(dummyComplexValueClassArg, slot.captured)

        verify { mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg) }
    }

    @Test
    fun `arg is ComplexValueClass, answers ComplexValueClass`() {
        val mock = mockk<DummyService> {
            every { argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg) } answers { dummyComplexValueClassReturn }
        }

        assertEquals(dummyComplexValueClassReturn, mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg))

        verify { mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg) }
    }

    @Test
    fun `arg is any(ComplexValueClass), answers ComplexValueClass`() {
        val mock = mockk<DummyService> {
            every { argComplexValueClassReturnComplexValueClass(any()) } answers { dummyComplexValueClassReturn }
        }

        assertEquals(dummyComplexValueClassReturn, mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg))

        verify { mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg) }
    }

    @Test
    fun `arg is slot(ComplexValueClass), answers ComplexValueClass`() {
        val slot = slot<ComplexValue>()

        val mock = mockk<DummyService> {
            every { argComplexValueClassReturnComplexValueClass(capture(slot)) } answers { dummyComplexValueClassReturn }
        }

        val result = mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg)

        assertEquals(dummyComplexValueClassReturn, result)

        assertEquals(dummyComplexValueClassArg, slot.captured)

        verify { mock.argComplexValueClassReturnComplexValueClass(dummyComplexValueClassArg) }
    }
    //</editor-fold>

    //<editor-fold desc="arg=Value Class, return=Wrapper">
    @Test
    fun `arg is ValueClass, returns Wrapper`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(dummyValueClassArg) } returns dummyValueWrapperReturn
        }

        assertEquals(dummyValueWrapperReturn, mock.argValueClassReturnWrapper(dummyValueClassArg))

        verify { mock.argValueClassReturnWrapper(dummyValueClassArg) }
    }

    @Test
    @Ignore // TODO support nested value classes https://github.com/mockk/mockk/issues/859
    fun `arg is any(ValueClass), returns Wrapper`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(any()) } returns dummyValueWrapperReturn
        }

        assertEquals(dummyValueWrapperArg, mock.argValueClassReturnWrapper(dummyValueClassArg))

        verify { mock.argValueClassReturnWrapper(dummyValueClassArg) }
    }

    @Test
    fun `arg is slot(ValueClass), returns Wrapper`() {
        val slot = slot<DummyValue>()

        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(capture(slot)) } returns dummyValueWrapperReturn
        }

        val result = mock.argValueClassReturnWrapper(dummyValueClassArg)

        assertEquals(dummyValueWrapperReturn, result)

        assertEquals(dummyValueClassArg, slot.captured)

        verify { mock.argValueClassReturnWrapper(dummyValueClassArg) }
    }

    @Test
    fun `arg is ValueClass, answers Wrapper`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(dummyValueClassArg) } answers { dummyValueWrapperReturn }
        }

        assertEquals(dummyValueWrapperReturn, mock.argValueClassReturnWrapper(dummyValueClassArg))

        verify { mock.argValueClassReturnWrapper(dummyValueClassArg) }
    }

    @Test
    fun `arg is any(ValueClass), answers Wrapper`() {
        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(any()) } answers { dummyValueWrapperReturn }
        }

        assertEquals(dummyValueWrapperReturn, mock.argValueClassReturnWrapper(dummyValueClassArg))

        verify { mock.argValueClassReturnWrapper(dummyValueClassArg) }
    }

    @Test
    fun `arg is slot(ValueClass), answers Wrapper`() {
        val slot = slot<DummyValue>()

        val mock = mockk<DummyService> {
            every { argValueClassReturnWrapper(capture(slot)) } answers { dummyValueWrapperReturn }
        }

        val result = mock.argValueClassReturnWrapper(dummyValueClassArg)

        assertEquals(dummyValueWrapperReturn, result)

        assertEquals(dummyValueClassArg, slot.captured)

        verify { mock.argValueClassReturnWrapper(dummyValueClassArg) }
    }
    //</editor-fold>

    //<editor-fold desc="arg=Wrapper, return=ValueClass">
    @Test
    fun `arg is Wrapper, returns ValueClass`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(dummyValueWrapperArg) } returns dummyValueClassReturn
        }

        assertEquals(dummyValueClassReturn, mock.argWrapperReturnValueClass(dummyValueWrapperArg))

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is any(Wrapper), returns ValueClass`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(any()) } returns dummyValueClassReturn
        }

        assertEquals(dummyValueClassReturn, mock.argWrapperReturnValueClass(dummyValueWrapperArg))

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    @Ignore // TODO support nested value classes https://github.com/mockk/mockk/issues/859
    fun `arg is slot(Wrapper), returns ValueClass`() {
        val slot = slot<DummyValueWrapper>()
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(capture(slot)) } returns dummyValueClassReturn
        }

        val result = mock.argWrapperReturnValueClass(dummyValueWrapperArg)

        assertEquals(dummyValueClassReturn, result)

        assertEquals(dummyValueWrapperArg, slot.captured)

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is Wrapper, answers ValueClass`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(dummyValueWrapperArg) } answers { dummyValueClassReturn }
        }

        assertEquals(dummyValueClassReturn, mock.argWrapperReturnValueClass(dummyValueWrapperArg))

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is any(Wrapper), answers ValueClass`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(any()) } answers { dummyValueClassReturn }
        }

        assertEquals(dummyValueClassReturn, mock.argWrapperReturnValueClass(dummyValueWrapperArg))

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }

    @Test
    @Ignore // TODO support nested value classes https://github.com/mockk/mockk/issues/859
    fun `arg is slot(Wrapper), answers ValueClass`() {
        val slot = slot<DummyValueWrapper>()

        val mock = mockk<DummyService> {
            every { argWrapperReturnValueClass(capture(slot)) } answers { dummyValueClassReturn }
        }

        val result = mock.argWrapperReturnValueClass(dummyValueWrapperArg)

        assertEquals(dummyValueClassReturn, result)

        assertEquals(dummyValueWrapperArg, slot.captured)

        verify { mock.argWrapperReturnValueClass(dummyValueWrapperArg) }
    }
    //</editor-fold>

    //<editor-fold desc="arg=Wrapper, return=Wrapper">
    @Test
    fun `arg is Wrapper, returns Wrapper`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(dummyValueWrapperArg) } returns dummyValueWrapperReturn
        }

        assertEquals(dummyValueWrapperReturn, mock.argWrapperReturnWrapper(dummyValueWrapperArg))

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is any(Wrapper), returns Wrapper`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(any()) } returns dummyValueWrapperReturn
        }

        assertEquals(dummyValueWrapperReturn, mock.argWrapperReturnWrapper(dummyValueWrapperArg))

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    @Ignore // TODO support nested value classes https://github.com/mockk/mockk/issues/859
    fun `arg is slot(Wrapper), returns Wrapper`() {
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
    fun `arg is Wrapper, answers Wrapper`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(dummyValueWrapperArg) } answers { dummyValueWrapperReturn }
        }

        assertEquals(dummyValueWrapperReturn, mock.argWrapperReturnWrapper(dummyValueWrapperArg))

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    fun `arg is any(Wrapper), answers Wrapper`() {
        val mock = mockk<DummyService> {
            every { argWrapperReturnWrapper(any()) } answers { dummyValueWrapperReturn }
        }

        assertEquals(dummyValueWrapperReturn, mock.argWrapperReturnWrapper(dummyValueWrapperArg))

        verify { mock.argWrapperReturnWrapper(dummyValueWrapperArg) }
    }

    @Test
    @Ignore // TODO support nested value classes https://github.com/mockk/mockk/issues/859
    fun `arg is slot(Wrapper), answers Wrapper`() {
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
    fun `receiver is String, return is ValueClass`() {
        val fn = mockk<String.() -> DummyValue>()

        every { "string".fn() } returns dummyValueClassReturn

        val result = "string".fn()

        assertEquals(dummyValueClassReturn, result)
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

    //<editor-fold desc="extension function on Wrapper">
    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is Wrapper, return is Wrapper`() {
        val fn = mockk<DummyValueWrapper.() -> DummyValueWrapper>()

        every { dummyValueWrapperArg.fn() } returns dummyValueWrapperArg

        val result = dummyValueWrapperArg.fn()

        assertEquals(dummyValueWrapperArg, result)
    }

    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is Wrapper, return is ValueClass`() {
        val fn = mockk<DummyValueWrapper.() -> DummyValue>()

        every { dummyValueWrapperArg.fn() } returns dummyValueClassReturn

        val result = dummyValueWrapperArg.fn()

        assertEquals(dummyValueClassArg, result)
    }

    @Test
    fun `receiver is Wrapper, return is String`() {
        val fn = mockk<DummyValueWrapper.() -> String>()

        every { dummyValueWrapperArg.fn() } returns "example"

        val result = dummyValueWrapperArg.fn()

        assertEquals("example", result)
    }
    //</editor-fold>

    //<editor-fold desc="extension function on ValueClass">
    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is ValueClass, return is Wrapper`() {
        val fn = mockk<DummyValue.() -> DummyValueWrapper>()

        every { dummyValueClassArg.fn() } returns dummyValueWrapperReturn

        val result = dummyValueClassArg.fn()

        assertEquals(dummyValueWrapperArg, result)
    }

    @Test
    @Ignore // TODO fix infinite loop
    fun `receiver is ValueClass, return is ValueClass`() {
        val fn = mockk<DummyValue.() -> DummyValue>()

        every { dummyValueClassArg.fn() } returns dummyValueClassReturn

        val result = dummyValueClassArg.fn()

        assertEquals(dummyValueClassReturn, result)
    }

    @Test
    fun `function returning nullable value class not boxed due to cast to another type`() {
        val mock = mockk<DummyService> {
            every { nullableValueClass() } returns DummyValue(2)
        }

        assertEquals(DummyValue(2), mock.nullableValueClass())
    }

    @Test
    fun `function returning nullable complex value class, returns value class`() {
        val expected = ComplexValue(UUID.fromString("c5744ead-302f-4e29-9f82-d10eb2a85ea3"))
        val mock = mockk<DummyService> {
            every { nullableComplexValueClass() } returns expected
        }

        assertEquals(expected, mock.nullableComplexValueClass())
    }

    @Test
    fun `function returning nullable nested value class, returns value class`() {
        val mock = mockk<DummyService> {
            every { nullableNestedValueClass() } returns DummyValueWrapper(DummyValue(10))
        }

        assertEquals(DummyValueWrapper(DummyValue(10)), mock.nullableNestedValueClass())
    }

    @Test
    fun `function returning nullable nested complex value class, returns value class`() {
        val expected = ComplexValue(UUID.fromString("c5744ead-302f-4e29-9f82-d10eb2a85ea3"))

        val mock = mockk<DummyService> {
            every { nullableNestedComplexValueClass() } returns DummyComplexValueWrapper(expected)
        }

        assertEquals(DummyComplexValueWrapper(expected), mock.nullableNestedComplexValueClass())
    }

    @Test
    fun `nullable complex value class field, returns value class`() {
        val expected = ComplexValue(UUID.fromString("c5744ead-302f-4e29-9f82-d10eb2a85ea3"))
        val mock = mockk<DummyService> {
            every { nullableComplexValueClassField } returns expected
        }

        assertEquals(expected, mock.nullableComplexValueClassField)
    }

    @Test
    fun `nullable nested value class field, returns value class`() {
        val mock = mockk<DummyService> {
            every { nullableNestedValueClassField } returns DummyValueWrapper(DummyValue(10))
        }

        assertEquals(DummyValueWrapper(DummyValue(10)), mock.nullableNestedValueClassField)
    }

    @Test
    fun `nullable nested complex value class field, returns value class`() {
        val expected = ComplexValue(UUID.fromString("c5744ead-302f-4e29-9f82-d10eb2a85ea3"))
        val mock = mockk<DummyService> {
            every { nullableNestedComplexValueClassField } returns DummyComplexValueWrapper(expected)
        }

        assertEquals(DummyComplexValueWrapper(expected), mock.nullableNestedComplexValueClassField)
    }

    @Test
    fun `nullable value class field is not boxed due to cast to another type`() {
        val mock = mockk<DummyService> {
            every { nullableValueClassField } returns DummyValue(2)
        }

        assertEquals(DummyValue(2), mock.nullableValueClassField)
    }

    @Test
    fun `receiver is ValueClass, return is String`() {
        val fn = mockk<DummyValue.() -> String>()

        every { dummyValueClassArg.fn() } returns "example"

        val result = dummyValueClassArg.fn()

        assertEquals("example", result)
    }

    @Test
    fun `result value`() {
        val givenResult = DummyValue(42)

        val mock = mockk<DummyService> {
            every { returnValueClass() } returns givenResult
        }

        val result = mock.returnValueClass()

        assertEquals(givenResult, result)
    }

    @Test
    fun `ensure no infinite recursion when mocking fun that returns value class`() {
        val f: () -> DummyValue = mockk()

        assertTimeoutPreemptively(Duration.ofMillis(500L)) {
            runCatching {
                every { f.invoke() } returns DummyValue(42)
            }
        }
    }

    @Test
    fun `spy class returning value class not boxed due to cast to another type`() {
        val f = spyk<DummyService>()
        val result = f.returnValueClassNotInlined() as DummyValue

        assertEquals(DummyValue(0), result)
    }

    @Test
    fun `mock class returning value class not boxed due to cast to another type`() {
        val f = mockk<DummyService>()
        every { f.returnValueClassNotInlined() } returns DummyValue(3)
        val result = f.returnValueClassNotInlined() as DummyValue

        assertEquals(DummyValue(3), result)
    }

    @Test
    fun `spy class returning value class boxed due to suspend function`() {
        val f = spyk<DummyService>()
        val result = runBlocking { f.returnValueClassSuspendNotInlined() }

        assertEquals(DummyValue(0), result)
    }

    @Test
    fun `mock class returning value class boxed due to suspend function`() {
        val f = mockk<DummyService>()
        coEvery { f.returnValueClassSuspendNotInlined() } returns DummyValue(3)
        val result = runBlocking { f.returnValueClassSuspendNotInlined() }

        assertEquals(DummyValue(3), result)
    }

    @Test
    fun `spy class returning complex value class not boxed due to suspend function`() {
        val f = spyk<DummyService>()
        val result = runBlocking { f.returnComplexValueClassSuspendInlined() }

        assertEquals(ComplexValue(UUID.fromString("c5744ead-302f-4e29-9f82-d10eb2a85ea3")), result)
    }

    @Test
    fun `mock class returning nested value class boxed due to suspend function`() {
        val expected = DummyValueWrapper(DummyValue(3))
        val f = mockk<DummyService>()
        coEvery { f.returnNestedValueClassSuspendNotInlined() } returns expected
        val result = runBlocking { f.returnNestedValueClassSuspendNotInlined() }

        assertEquals(expected, result)
    }

    @Test
    fun `mock class returning nested complex value class not boxed due to suspend function`() {
        val expected =
            DummyComplexValueWrapper(ComplexValue(UUID.fromString("bca61f8d-ba4d-475f-8dc6-08b943836998")))
        val f = mockk<DummyService>()
        coEvery { f.returnNestedComplexValueClassSuspendNotInlined() } returns expected
        val result = runBlocking { f.returnNestedComplexValueClassSuspendNotInlined() }

        assertEquals(expected, result)
    }

    @Test
    fun `mock class returning complex value class not boxed due to suspend function`() {
        val f = mockk<DummyService>()
        coEvery { f.returnComplexValueClassSuspendInlined() } returns ComplexValue(UUID.fromString("bca61f8d-ba4d-475f-8dc6-08b943836998"))
        val result = runBlocking { f.returnComplexValueClassSuspendInlined() }

        assertEquals(ComplexValue(UUID.fromString("bca61f8d-ba4d-475f-8dc6-08b943836998")), result)
    }

    companion object {

        @JvmInline
        value class DummyValue(val value: Int) {
            // field without backing field
            val text: String get() = value.toString()
        }

        @JvmInline
        value class ComplexValue(val value: UUID) {
            val text: String get() = value.toString()
        }

        @JvmInline
        value class DummyValueWrapper(val value: DummyValue)

        @JvmInline
        value class DummyComplexValueWrapper(val value: ComplexValue)

        @Suppress("UNUSED_PARAMETER")
        class DummyService {
            val valueClassField = DummyValue(0)
            val nullableValueClassField: DummyValue? = null
            val nullableComplexValueClassField: ComplexValue? = null
            val nullableNestedValueClassField: DummyValueWrapper? = null
            val nullableNestedComplexValueClassField: DummyComplexValueWrapper? = null

            fun argWrapperReturnWrapper(wrapper: DummyValueWrapper): DummyValueWrapper =
                DummyValueWrapper(DummyValue(0))

            fun argWrapperReturnValueClass(wrapper: DummyValueWrapper): DummyValue =
                DummyValue(0)

            fun argValueClassReturnWrapper(valueClass: DummyValue): DummyValueWrapper =
                DummyValueWrapper(valueClass)

            fun argValueClassReturnValueClass(valueClass: DummyValue): DummyValue =
                DummyValue(0)

            fun argComplexValueClassReturnComplexValueClass(complexValue: ComplexValue): ComplexValue =
                ComplexValue(UUID.fromString("7dea337b-ce0b-4e25-9788-79e708aadc33"))

            fun returnValueClass(): DummyValue =
                DummyValue(0)

            // Note the value class is not inlined in this case due to being cast to another type
            fun returnValueClassNotInlined(): Any = DummyValue(0)

            @Suppress("RedundantSuspendModifier")
            suspend fun returnValueClassSuspendNotInlined(): DummyValue = DummyValue(0)

            @Suppress("RedundantSuspendModifier")
            suspend fun returnComplexValueClassSuspendInlined(): ComplexValue =
                ComplexValue(UUID.fromString("c5744ead-302f-4e29-9f82-d10eb2a85ea3"))

            @Suppress("RedundantSuspendModifier")
            suspend fun returnNestedValueClassSuspendNotInlined(): DummyValueWrapper =
                DummyValueWrapper(DummyValue(0))

            @Suppress("RedundantSuspendModifier")
            suspend fun returnNestedComplexValueClassSuspendNotInlined(): DummyComplexValueWrapper =
                DummyComplexValueWrapper(ComplexValue(UUID.fromString("c5744ead-302f-4e29-9f82-d10eb2a85ea3")))

            fun argNoneReturnsUInt(): UInt = 123u

            fun nullableValueClass(): DummyValue? = null

            fun nullableComplexValueClass(): ComplexValue? = null

            fun nullableNestedValueClass(): DummyValueWrapper? = null

            fun nullableNestedComplexValueClass(): DummyComplexValueWrapper? = null
        }
    }
}
