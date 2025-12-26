package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test related to GitHub issue #386
 */
class CollectionsReturnOnRelaxedMockTest {
    class ReturningCollections {
        fun getList(): List<Any> = listOf()

        fun getMap(): Map<Any, Any> = mapOf()

        fun getSet(): Set<Any> = setOf()

        fun getArrayList(): ArrayList<Any> = arrayListOf()

        fun getHashMap(): HashMap<Any, Any> = hashMapOf()

        fun getHashSet(): HashSet<Any> = hashSetOf()
    }

    @RelaxedMockK
    private lateinit var returningCollections: ReturningCollections

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun getList() {
        val list = returningCollections.getList()
        assertEquals(0, list.size)
        assertTrue(list.isEmpty())
    }

    @Test
    fun getMap() {
        val map = returningCollections.getMap()
        assertEquals(0, map.size)
        assertTrue(map.isEmpty())
    }

    @Test
    fun getSet() {
        val set = returningCollections.getSet()
        assertEquals(0, set.size)
        assertTrue(set.isEmpty())
    }

    @Test
    fun getArrayList() {
        val arraylist = returningCollections.getArrayList()
        assertEquals(0, arraylist.size)
        assertTrue(arraylist.isEmpty())
    }

    @Test
    fun getHashMap() {
        val hashmap = returningCollections.getHashMap()
        assertEquals(0, hashmap.size)
        assertTrue(hashmap.isEmpty())
    }

    @Test
    fun getHashSet() {
        val hashset = returningCollections.getHashSet()
        assertEquals(0, hashset.size)
        assertTrue(hashset.isEmpty())
    }
}
