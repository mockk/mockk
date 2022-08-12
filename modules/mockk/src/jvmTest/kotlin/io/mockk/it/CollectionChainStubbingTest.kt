package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Regression tests for GitHub issue #565.
class CollectionChainStubbingTest {

    private interface ReturningCollections {
        val list: List<Int>
        val map: Map<String, Int>
        val set: Set<Int>
        val arrayList: ArrayList<Int>
        val hashMap: Map<String, Int>
        val hashSet: HashSet<Int>
    }

    @Test
    fun list() {
        val mock = mockk<ReturningCollections>()
        every { mock.list[0] } returns 1

        assertEquals(1, mock.list[0])
    }

    @Test
    fun map() {
        val mock = mockk<ReturningCollections>()
        every { mock.map["foo"] } returns 1

        assertEquals(1, mock.map["foo"])
    }

    @Test
    fun set() {
        val mock = mockk<ReturningCollections>()
        every { mock.set.contains(1) } returns true

        assertTrue(mock.set.contains(1))
    }

    @Test
    fun arrayList() {
        val mock = mockk<ReturningCollections>()
        every { mock.arrayList[0] } returns 1

        assertEquals(1, mock.arrayList[0])
    }

    @Test
    fun hashMap() {
        val mock = mockk<ReturningCollections>()
        every { mock.hashMap["foo"] } returns 1

        assertEquals(1, mock.hashMap["foo"])
    }

    @Test
    fun hashSet() {
        val mock = mockk<ReturningCollections>()
        every { mock.hashSet.contains(0) } returns true

        assertTrue(mock.hashSet.contains(0))
    }
}