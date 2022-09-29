package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals


class SealedClassTest {

    @Test
    fun serviceReturnsSealedClassImpl() {
        val factory = mockk<Factory> {
            every { create() } returns Leaf(1)
        }

        val result = factory.create()

        assertEquals(Leaf(1), result)
    }

    @Test
    fun serviceAnswersSealedClassImpl() {
        val factory = mockk<Factory> {
            every { create() } answers { Leaf(1) }
        }

        val result = factory.create()

        assertEquals(Leaf(1), result)
    }

    @Test
    fun serviceReturnsSealedClassImplWithAnyMatcher() {
        val factory = mockk<Factory> {
            every { copy(any()) } returns Leaf(1)
        }

        val result = factory.create()

        assertEquals(Leaf(1), result)
    }

    @Test
    fun serviceAnswersSealedClassImplWithAnyMatcher() {
        val factory = mockk<Factory> {
            every { copy(any()) } answers { Leaf(1) }
        }

        val result = factory.create()

        assertEquals(Leaf(1), result)
    }

    companion object {

        sealed class Node

        data class Root(val id: Int) : Node()
        data class Leaf(val id: Int) : Node()

        interface Factory {
            fun create(): Node
            fun copy(node: Node): Node
        }

        class FactoryImpl : Factory {
            override fun create(): Node = Root(0)
            override fun copy(node: Node): Node = node
        }

    }
}
