package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MyData(
    val str: String,
)

fun MyData.ext(): String = "$str - ext"

class OtherData(
    val value: Int,
)

fun takesData(data: Any): String = "real $data"

fun takesDataAndSuffix(
    data: Any,
    suffix: String,
): String = "real $data $suffix"

class Relay {
    fun relay(data: Any): String = "relay $data"
}

class Issue874ExtensionOnConstructorMockTest {
    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `extension function on constructed mock is intercepted`() {
        mockkStatic("io.mockk.it.Issue874ExtensionOnConstructorMockTestKt")
        mockkConstructor(MyData::class)
        every { anyConstructed<MyData>().str } returns "Mock Str"
        every { anyConstructed<MyData>().ext() } returns "Mock Ext"

        val myData = MyData("abc")

        assertEquals("Mock Ext", myData.ext())
    }

    @Test
    fun `extension function on spy mock is intercepted`() {
        mockkStatic("io.mockk.it.Issue874ExtensionOnConstructorMockTestKt")
        val myData = io.mockk.spyk(MyData("abc"))
        every { myData.str } returns "Mock Str"
        every { myData.ext() } returns "Mock Ext"

        assertEquals("Mock Ext", myData.ext())
    }

    @Test
    fun `placeholder argument stays specific to its own constructor mock`() {
        mockkStatic("io.mockk.it.Issue874ExtensionOnConstructorMockTestKt")
        mockkConstructor(MyData::class)
        mockkConstructor(OtherData::class)
        every { takesData(anyConstructed<MyData>()) } returns "mocked-data"
        every { takesData(anyConstructed<OtherData>()) } returns "mocked-other"

        assertEquals("mocked-data", takesData(MyData("abc")))
        assertEquals("mocked-other", takesData(OtherData(7)))
    }

    @Test
    fun `placeholder argument passed to a plain instance method of another mock is matched`() {
        val relay = mockk<Relay>()
        mockkConstructor(MyData::class)
        every { relay.relay(anyConstructed<MyData>()) } returns "relayed"

        assertEquals("relayed", relay.relay(MyData("abc")))
        assertFailsWith<MockKException> {
            relay.relay("plain")
        }
    }

    @Test
    fun `placeholder argument combined with another matcher in the same call`() {
        mockkStatic("io.mockk.it.Issue874ExtensionOnConstructorMockTestKt")
        mockkConstructor(MyData::class)
        every { takesDataAndSuffix(anyConstructed<MyData>(), eq("known")) } returns "mocked-known"

        assertEquals("mocked-known", takesDataAndSuffix(MyData("abc"), "known"))
        assertTrue(
            takesDataAndSuffix(MyData("other"), "other").startsWith("real"),
            "the eq() part must not match, so the real static function has to run",
        )
    }

    @Test
    fun `verify with placeholder argument captures every constructed instance`() {
        mockkStatic("io.mockk.it.Issue874ExtensionOnConstructorMockTestKt")
        mockkConstructor(MyData::class)
        every { takesData(anyConstructed<MyData>()) } returns "mocked"

        val first = MyData("first")
        val second = MyData("second")
        takesData(first)
        takesData(second)

        val captured = mutableListOf<MyData>()
        verify(exactly = 2) { takesData(capture(captured)) }
        assertEquals(listOf(first, second), captured)
    }
}
