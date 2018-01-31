package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinClassWithEnumMemberTest {
    class KotlinClassWithEnumMember {
        var type: EnumType? = null

        enum class EnumType {
            ONE, TWO, THREE
        }

        var typeString = "test"
    }

    @MockK
    lateinit var testObj: KotlinClassWithEnumMember

    @BeforeTest
    fun setUp() = MockKAnnotations.init(this)

    @Test
    fun testMockingEnumMemberInClass() {
        val mockedClass = mockk<KotlinClassWithEnumMember>()
        every { mockedClass.type } returns KotlinClassWithEnumMember.EnumType.ONE
        assertEquals(KotlinClassWithEnumMember.EnumType.ONE, mockedClass.type, "Enum returned does not match mocked response")
    }

    @Test
    fun testMockingEnumMemberInLateinitClass() {
        every { testObj.type } returns KotlinClassWithEnumMember.EnumType.ONE
        assertEquals(KotlinClassWithEnumMember.EnumType.ONE, testObj.type, "Enum returned does not match mocked response")
    }

    @Test
    fun testMockingStringMemberInLateinitClass() {
        every { testObj.typeString } returns "teststring"
        assertEquals("teststring", testObj.typeString, "Enum returned does not match mocked response")
    }
}