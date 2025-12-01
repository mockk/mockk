package io.mockk.ait

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test for issue #1450: java.lang.StackOverflowError when using every/verify
 * with a method returning ArrayList.
 *
 * This test verifies that mocking methods that return ArrayList or other concrete
 * collection types does not cause StackOverflowError.
 */
@RunWith(AndroidJUnit4::class)
class ArrayListReturnTypeTest {

    class ListTest {
        fun getOneItem(v: String): String {
            return v
        }

        fun getListItem(v: String): ArrayList<String> {
            return arrayListOf(v)
        }

        fun getHashSetItem(v: String): HashSet<String> {
            return hashSetOf(v)
        }

        fun getLinkedListItem(v: String): java.util.LinkedList<String> {
            return java.util.LinkedList<String>().apply { add(v) }
        }
    }

    @Test
    fun testMockArrayListReturnType() {
        val mockTest = mockk<ListTest>()

        // This should not throw StackOverflowError
        every { mockTest.getListItem(any()) } returns ArrayList()

        val result = mockTest.getListItem("test")

        assertTrue(result.isEmpty())
    }

    @Test
    fun testMockArrayListReturnTypeWithElements() {
        val mockTest = mockk<ListTest>()

        // This should not throw StackOverflowError
        every { mockTest.getListItem(any()) } returns arrayListOf("element1", "element2")

        val result = mockTest.getListItem("test")

        assertEquals(2, result.size)
        assertEquals("element1", result[0])
        assertEquals("element2", result[1])
    }

    @Test
    fun testVerifyArrayListReturnType() {
        val mockTest = mockk<ListTest>(relaxed = true)

        mockTest.getListItem("test")

        // This should not throw StackOverflowError
        verify { mockTest.getListItem(any()) }
    }

    @Test
    fun testMockHashSetReturnType() {
        val mockTest = mockk<ListTest>()

        // This should not throw StackOverflowError
        every { mockTest.getHashSetItem(any()) } returns HashSet()

        val result = mockTest.getHashSetItem("test")

        assertTrue(result.isEmpty())
    }

    @Test
    fun testMockLinkedListReturnType() {
        val mockTest = mockk<ListTest>()

        // This should not throw StackOverflowError
        every { mockTest.getLinkedListItem(any()) } returns java.util.LinkedList()

        val result = mockTest.getLinkedListItem("test")

        assertTrue(result.isEmpty())
    }

    @Test
    fun testMockStringReturnTypeStillWorks() {
        val mockTest = mockk<ListTest>()

        every { mockTest.getOneItem(any()) } returns "mocked"

        val result = mockTest.getOneItem("test")

        assertEquals("mocked", result)
    }
}
