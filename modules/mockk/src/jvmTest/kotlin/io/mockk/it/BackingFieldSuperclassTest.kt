package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Constant getter stubs that have to be visible through direct reads of the backing field.
 *
 * For a `final` property with a backing field the Kotlin compiler does not call the getter
 * everywhere: a callable reference to the property is inlined as a `getfield`. A stub set
 * through `every { ... } returns ...` is mirrored into the mock's backing field for exactly
 * that reason, but the field lives in the class that declares the property, which is often
 * a superclass of the mocked class rather than the class itself
 * (https://github.com/mockk/mockk/issues/1371).
 */
class BackingFieldSuperclassTest {
    abstract class Base(
        dependencies: List<Base>,
    ) {
        val dependencyValues = dependencies.map(Base::value)

        val value = "base-value"
    }

    class Child : Base(emptyList())

    class Holder(
        child: Child,
    ) : Base(listOf(child))

    abstract class GrandBase(
        dependencies: List<GrandBase>,
    ) {
        val dependencyValues = dependencies.map(GrandBase::value)

        val value = "grand-value"
    }

    open class MiddleChild : GrandBase(emptyList())

    class LeafChild : MiddleChild()

    class GrandHolder(
        child: LeafChild,
    ) : GrandBase(listOf(child))

    abstract class FlagBase(
        dependencies: List<FlagBase>,
    ) {
        val dependencyFlags = dependencies.map(FlagBase::isActive)

        val isActive = true
    }

    class FlagChild : FlagBase(emptyList())

    class FlagHolder(
        child: FlagChild,
    ) : FlagBase(listOf(child))

    class Standalone(
        dependencies: List<Standalone>,
    ) {
        val dependencyValues = dependencies.map(Standalone::value)

        val value = "standalone-value"
    }

    @Test
    fun `stubbed inherited property is returned when the compiler reads the backing field directly`() {
        val child =
            mockk<Child> {
                every { value } returns "stubbed"
            }

        val holder = Holder(child)

        assertEquals(listOf("stubbed"), holder.dependencyValues)
    }

    @Test
    fun `stubbed property declared two levels up is mirrored into the grandparent field`() {
        val leaf =
            mockk<LeafChild> {
                every { value } returns "stubbed"
            }

        val holder = GrandHolder(leaf)

        assertEquals(listOf("stubbed"), holder.dependencyValues)
    }

    @Test
    fun `stubbed is-prefixed inherited property is mirrored into the superclass field`() {
        val child =
            mockk<FlagChild> {
                every { isActive } returns false
            }

        val holder = FlagHolder(child)

        assertEquals(listOf(false), holder.dependencyFlags)
    }

    @Test
    fun `stubbed property declared in the mock itself is still mirrored`() {
        val other =
            mockk<Standalone> {
                every { value } returns "stubbed"
            }

        val standalone = Standalone(listOf(other))

        assertEquals(listOf("stubbed"), standalone.dependencyValues)
    }
}
