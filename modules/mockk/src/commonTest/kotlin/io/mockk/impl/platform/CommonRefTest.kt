package io.mockk.impl.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonRefTest {
    @Test
    fun givenTwoObjectRefsWhenCheckedEqualityThenAreSame() {
        val obj = Any()
        val ref1 = CommonRef(obj)
        val ref2 = CommonRef(obj)

        assertEquals(ref1, ref2)
    }

    @Test
    fun givenTwoObjectsRefsWhenCheckedEqualityThenAreDifferent() {
        val obj1 = Any()
        val obj2 = Any()
        val ref1 = CommonRef(obj1)
        val ref2 = CommonRef(obj2)

        assertTrue(ref1 != ref2)
    }

    @Test
    fun givenTwoObjectRefsWhenCheckedHashCodesThenAreSame() {
        val obj = Any()
        val ref1 = CommonRef(obj)
        val ref2 = CommonRef(obj)
        val hash1 = ref1.hashCode()
        val hash2 = ref2.hashCode()

        assertEquals(hash1, hash2)
    }

    @Test
    fun givenTwoIntRefsWhenCheckedEqualityThenAreSame() {
        val obj = 3
        val ref1 = CommonRef(obj)
        val ref2 = CommonRef(obj)

        assertEquals(ref1, ref2)
    }

    @Test
    fun givenTwoIntsRefsWhenCheckedEqualityThenAreDifferent() {
        val val1 = 3
        val val2 = 4
        val ref1 = CommonRef(val1)
        val ref2 = CommonRef(val2)

        assertTrue(ref1 != ref2)
    }

    @Test
    fun givenTwoIntRefsWhenCheckedHashCodesThenAreSame() {
        val value = 3
        val ref1 = CommonRef(value)
        val ref2 = CommonRef(value)
        val hash1 = ref1.hashCode()
        val hash2 = ref2.hashCode()

        assertEquals(hash1, hash2)
    }

}
