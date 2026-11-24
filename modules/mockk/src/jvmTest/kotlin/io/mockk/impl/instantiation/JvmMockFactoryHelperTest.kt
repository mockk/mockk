package io.mockk.impl.instantiation

import io.mockk.JvmVarArgsCls
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.instantiation.JvmMockFactoryHelper.descriptionCache
import io.mockk.impl.instantiation.JvmMockFactoryHelper.inlineCache
import io.mockk.impl.instantiation.JvmMockFactoryHelper.isKotlinInline
import io.mockk.impl.instantiation.JvmMockFactoryHelper.toDescription
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JvmMockFactoryHelperTest {
    @Test
    fun kotlinVarArgPosition() {
        val clazz = SomeClass::class.java

        assertEquals(
            -1,
            clazz.getMethod("noArg").toDescription().varArgsArg,
        )
        assertEquals(
            -1,
            clazz.getMethod("noVarArg", Unit.javaClass).toDescription().varArgsArg,
        )
        assertEquals(
            0,
            clazz.getMethod("varArgFirst", Array<Unit>::class.java, Unit.javaClass).toDescription().varArgsArg,
        )
        assertEquals(
            1,
            clazz.getMethod("varArgLast", Unit.javaClass, Array<Unit>::class.java).toDescription().varArgsArg,
        )
    }

    @Test
    fun javaVarArgPosition() {
        val clazz = JvmVarArgsCls::class.java

        assertEquals(
            1,
            clazz.getMethod("varArgsOp", Int::class.javaPrimitiveType, IntArray::class.java).toDescription().varArgsArg,
        )
    }

    /**
     * A regenerated subclass proxy class captures fresh `Method` objects for the same logical
     * methods (issue #1627), and modern JDKs hand out a fresh copy on every `getDeclaredMethod`
     * call anyway - two lookups below are two distinct, equal instances, the exact shape of the
     * production scenario. The description of the first instance must be served to the second
     * one, so the kotlin-reflect work behind it runs once per logical method.
     */
    @Test
    fun descriptionIsMemoizedAcrossEqualButDistinctMethodInstances() {
        val method = SomeClass::class.java.getDeclaredMethod("noArg")
        val regenerated = SomeClass::class.java.getDeclaredMethod("noArg")

        assertNotSame(method, regenerated)
        assertEquals(method, regenerated)

        assertSame(
            method.toDescription(),
            regenerated.toDescription(),
        )
    }

    @Test
    fun descriptionCacheKeysOnMethodEquality() {
        val method = SomeClass::class.java.getDeclaredMethod("noVarArg", Unit.javaClass)
        val regenerated = SomeClass::class.java.getDeclaredMethod("noVarArg", Unit.javaClass)

        method.toDescription()

        assertTrue(
            descriptionCache.containsKey(regenerated),
            "an equal Method instance of the same logical method must hit the description cache",
        )
    }

    @Test
    fun distinctLogicalMethodsAreNotMerged() {
        val method = SomeClass::class.java.getDeclaredMethod("noArg")
        val overload = SomeClass::class.java.getDeclaredMethod("noVarArg", Unit.javaClass)

        assertNotSame(method.toDescription(), overload.toDescription())
    }

    /**
     * The inline check runs on every mocked call; its result must be memoized per logical
     * method instead of re-running kotlin-reflect each time (issue #1627).
     */
    @Test
    fun inlineDetectionIsMemoizedPerMethod() {
        val plain = SomeClass::class.java.getDeclaredMethod("noArg")
        assertFalse(plain.isKotlinInline())

        val inlineFun = SomeClass::class.java.getDeclaredMethod("inlineFun", Function0::class.java)
        assertTrue(inlineFun.isKotlinInline())

        assertEquals(false, inlineCache[plain])
        assertEquals(true, inlineCache[inlineFun])
    }

    /**
     * End to end check: mocked calls flow through the handler, whose first two lookups are the
     * inline check and the description, and both must be served from the caches by the time the
     * same method is invoked again.
     */
    @Test
    fun repeatedMockInvocationsAreServedFromTheCaches() {
        val mock = mockk<SomeClass>()

        try {
            every { mock.noArg() } just Runs
            repeat(100) { mock.noArg() }
        } finally {
            unmockkAll()
        }

        val method = SomeClass::class.java.getDeclaredMethod("noArg")

        assertTrue(
            descriptionCache.containsKey(method),
            "the invoked method must be present in the description cache",
        )
        assertTrue(
            inlineCache.containsKey(method),
            "the invoked method must be present in the inline cache",
        )
    }
}

@Suppress("UNUSED_PARAMETER", "unused")
internal class SomeClass {
    fun noArg() {}

    fun noVarArg(arg0: Unit) {}

    fun varArgFirst(
        vararg arg0: Unit,
        arg1: Unit,
    ) {}

    fun varArgLast(
        arg0: Unit,
        vararg arg1: Unit,
    ) {}

    inline fun inlineFun(block: () -> Unit) {
        block()
    }
}
