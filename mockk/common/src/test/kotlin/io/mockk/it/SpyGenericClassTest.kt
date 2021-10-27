package io.mockk.it

import io.mockk.mockk
import io.mockk.spyk
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@Ignore
class SpyGenericClassTest {
    /**
     * I want to use spyk for class with generic. It produces StackOverflowError.
     * Issue is bridge method handling.
     *
     * Verifies issue #81.
     */
    @Test
    fun bridgeMethods() {
        val view = mockk<SomeView>()
        val childClazz = spyk(ChildClazz(), recordPrivateCalls = true)
        childClazz.foobar(view)
        assertEquals(true, childClazz.called)
    }

    private interface ParentView

    private interface SomeView : ParentView

    private interface ParentInterface<V : ParentView> {
        fun foobar(view: V)
    }

    private abstract class ParentClazz<V : ParentView> : ParentInterface<V> {
        override fun foobar(view: V) {
            throw RuntimeException()
        }
    }

    private class ChildClazz : ParentClazz<SomeView>() {
        var called: Boolean = false

        override fun foobar(view: SomeView) {
            super.foobar(view)
            called = true
        }
    }
}
