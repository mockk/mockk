package io.mockk.it

import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MyData(
    val str: String,
)

fun MyData.ext(): String = "$str - ext"

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
}
