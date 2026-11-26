package io.mockk

import kotlin.Metadata
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Subclass proxies carry the mocked type's annotations over, but its Kotlin `@Metadata`
 * stays out: kotlin-reflect trusts `@Metadata` over the actual Java hierarchy, so a proxy
 * keeping it reflects as the mocked type instead of as itself (#1189).
 */
class SubclassProxyReflectionTest {
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Marker

    @Marker
    open class AnnotatedKotlinCls(
        val value: String,
    )

    @Marker
    interface AnnotatedKotlinIface {
        fun answer(): Int
    }

    @Marker
    abstract class AbstractAnnotatedKotlinCls {
        abstract fun answer(): Int
    }

    object SingletonCls {
        fun value(): Int = 42
    }

    @Test
    fun `mock of an annotated Kotlin interface keeps the annotation but drops @Metadata`() {
        val mock = mockk<AnnotatedKotlinIface>()

        assertFalse(
            mock.javaClass.isAnnotationPresent(Metadata::class.java),
            "proxy class ${mock.javaClass.name} should not carry the mocked type's @Metadata",
        )
        assertTrue(
            mock.javaClass.isAnnotationPresent(Marker::class.java),
            "proxy class ${mock.javaClass.name} should keep the mocked type's own annotations",
        )
        assertTrue(
            mock::class.isSubclassOf(AnnotatedKotlinIface::class),
            "superclasses=${mock::class.superclasses}",
        )
    }

    @Test
    fun `mock of an annotated abstract Kotlin class keeps the annotation but drops @Metadata`() {
        val mock = mockk<AbstractAnnotatedKotlinCls>()

        assertFalse(
            mock.javaClass.isAnnotationPresent(Metadata::class.java),
            "proxy class ${mock.javaClass.name} should not carry the mocked type's @Metadata",
        )
        assertTrue(
            mock.javaClass.isAnnotationPresent(Marker::class.java),
            "proxy class ${mock.javaClass.name} should keep the mocked type's own annotations",
        )
        assertTrue(
            mock::class.isSubclassOf(AbstractAnnotatedKotlinCls::class),
            "superclasses=${mock::class.superclasses}",
        )
    }

    @Test
    fun `spy of a Kotlin class reflects as a subclass of it`() {
        val spy = spyk(AnnotatedKotlinCls("x"))

        assertTrue(
            spy::class.isSubclassOf(AnnotatedKotlinCls::class),
            "superclasses=${spy::class.superclasses}",
        )
        assertTrue(
            spy::class.memberProperties.any { it.name == "value" },
            "memberProperties=${spy::class.memberProperties.map { it.name }}",
        )
    }

    @Test
    fun `mock of a Kotlin object class reflects as a subclass of it`() {
        val mock = mockk<SingletonCls>()

        assertTrue(
            mock::class.isSubclassOf(SingletonCls::class),
            "superclasses=${mock::class.superclasses}",
        )
    }

    @Test
    fun `relaxed mock of a Kotlin class exposes its properties through reflection`() {
        val mock = mockk<AnnotatedKotlinCls>(relaxed = true)

        assertTrue(
            mock::class.isSubclassOf(AnnotatedKotlinCls::class),
            "superclasses=${mock::class.superclasses}",
        )
        assertTrue(
            mock::class.memberProperties.any { it.name == "value" },
            "memberProperties=${mock::class.memberProperties.map { it.name }}",
        )
    }

    @Test
    fun `mock of an annotated abstract Java class keeps its annotation`() {
        val mock = mockk<AnnotatedJavaCls>()

        assertTrue(
            mock.javaClass.isAnnotationPresent(AnnotatedJavaCls.Marker::class.java),
            "proxy class ${mock.javaClass.name} should keep the mocked type's own annotations",
        )
    }
}
