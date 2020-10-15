package io.mockk.impl.recording

import io.mockk.AllAnyMatcher
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignatureMatchersListTest {

    @Test
    fun shouldAddMatcherForSignature() {
        // given
        val map = SignatureMatchersList()
        val signedMatcher = mockk<SignedMatcher>(relaxed = true)
        val signature = listOf(signedMatcher.signature)
        val matcher = signedMatcher.matcher

        // when
        map.add(signature, matcher)
        map.add(signature, matcher)

        // then
        assertTrue { map.isNotEmpty() }
    }

    @Test
    fun shouldNotContainAnyMatchers() {
        // given
        val map = SignatureMatchersList()

        // when && then
        assertFalse { map.isNotEmpty() }
    }

    @Test
    fun shouldNotRemoveFromEmptyList() {
        // given
        val map = SignatureMatchersList()
        val signedMatcher = mockk<SignedMatcher>(relaxed = true)
        val signature = listOf(signedMatcher.signature)

        // when
        val matcher = map.remove(signature)

        // then
        assertFalse { map.isNotEmpty() }
        assertNull(matcher)
    }

    @Test
    fun shouldRemoveFromList() {
        // given
        val map = SignatureMatchersList()
        val signedMatcher1 = mockk<SignedMatcher>(relaxed = true)
        val signedMatcher2 = mockk<SignedMatcher>(relaxed = true)
        val signature1 = listOf(signedMatcher1.signature)
        val signature2 = listOf(signedMatcher2.signature)
        val matcher1 = signedMatcher1.matcher
        val matcher2 = signedMatcher2.matcher

        // when
        map.add(signature1, matcher1)
        map.add(signature2, matcher2)

        // and
        val matcher = map.remove(signature1)

        // then
        assertTrue { map.isNotEmpty() }
        assertEquals(matcher, matcher1)
    }

    @Test
    fun shouldPrintListCorrectly() {
        // given
        val list = SignatureMatchersList()
        val signedMatcher = mockk<SignedMatcher>(relaxed = true)
        val signature = listOf(signedMatcher.signature)
        val matcher = AllAnyMatcher<Any>()
        val expectedMatchersListAsString = "[$matcher]"

        // when
        list.add(signature, matcher)

        // and
        val matchersAsString = list.toString()

        // then
        assertEquals(matchersAsString, expectedMatchersListAsString)
    }
}