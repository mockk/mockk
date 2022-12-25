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
        fun getList(): List<Any> {
            return listOf()
        }
        fun getMap(): Map<Any, Any> {
            return mapOf()
        }
        fun getSet(): Set<Any> {
            return setOf()
        }
        fun getArrayList(): ArrayList<Any> {
            return arrayListOf()
        }
        fun getHashMap(): HashMap<Any, Any> {
            return hashMapOf()
        }
        fun getHashSet(): HashSet<Any> {
            return hashSetOf()
        }
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
