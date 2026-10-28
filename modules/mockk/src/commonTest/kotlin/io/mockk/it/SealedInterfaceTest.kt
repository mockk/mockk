package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class SealedInterfaceTest {
    @Test
    fun serviceReturnsSealedClassImpl() {
        val factory =
            mockk<Factory> {
                every { create() } returns Leaf(1)
            }

        val result = factory.create()

        assertEquals(Leaf(1), result)
    }

    @Test
    fun serviceAnswersSealedClassImpl() {
        val factory =
            mockk<Factory> {
                every { create() } answers { Leaf(1) }
            }

        val result = factory.create()

        assertEquals(Leaf(1), result)
    }

    @Test
    fun serviceTakesSealedInterfaceAsInput() {
        val formattedNode = "Formatted node"
        val factory =
            mockk<Factory> {
                every { format(any()) } answers { formattedNode }
            }

        val result = factory.format(Root(0))

        assertEquals(formattedNode, result)
    }

    @Test
    fun serviceTakesSeveralSealedInterfacesWithValueClassImplAsInput() {
        val factory =
            mockk<Factory> {
                every { combine(any(), any()) } returns "combined"
            }

        val result = factory.combine(Leaf(1), Root(2))

        assertEquals("combined", result)
    }

    @Test
    fun serviceTakesSeveralSealedInterfacesWithOnlyValueClassImplsAsInput() {
        val factory =
            mockk<Factory> {
                every { combineIds(any(), any()) } returns "combined"
            }

        val result = factory.combineIds(LongId(1), TextId("2"))

        assertEquals("combined", result)
    }

    @Test
    fun serviceTakesSealedInterfaceWithValidatedValueClassImplAsInput() {
        val factory =
            mockk<Factory> {
                every { combineChecked(any(), PositiveId(2)) } returns "combined"
            }

        val result = factory.combineChecked(PositiveId(1), PositiveId(2))

        assertEquals("combined", result)
    }

    companion object {
        sealed interface Node

        data class Root(
            val id: Int,
        ) : Node

        data class Leaf(
            val id: Int,
        ) : Node

        @JvmInline
        value class Branch(
            val id: Int,
        ) : Node

        sealed interface Id

        @JvmInline
        value class LongId(
            val value: Long,
        ) : Id

        @JvmInline
        value class TextId(
            val value: String,
        ) : Id

        sealed interface CheckedId

        @JvmInline
        value class PositiveId(
            val value: Int,
        ) : CheckedId {
            init {
                require(value > 0)
            }
        }

        interface Factory {
            fun create(): Node

            fun format(node: Node): String

            fun combine(
                first: Node,
                second: Node,
            ): String

            fun combineIds(
                first: Id,
                second: Id,
            ): String

            fun combineChecked(
                first: CheckedId,
                second: CheckedId,
            ): String
        }

        class FactoryImpl : Factory {
            override fun create(): Node = Root(0)

            override fun format(node: Node): String = node.toString()

            override fun combine(
                first: Node,
                second: Node,
            ): String = "$first $second"

            override fun combineIds(
                first: Id,
                second: Id,
            ): String = "$first $second"

            override fun combineChecked(
                first: CheckedId,
                second: CheckedId,
            ): String = "$first $second"
        }
    }
}
