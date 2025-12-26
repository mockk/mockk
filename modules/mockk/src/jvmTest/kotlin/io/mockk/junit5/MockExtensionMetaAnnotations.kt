package io.mockk.junit5

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

annotation class Level1

@Level1
annotation class Level2

@Level2
annotation class Level3

class MockExtensionMetaAnnotations {
    @Test
    fun `should detect all annotation levels via meta-annotation`() {
        @Level3
        class AnnotatedClass

        val result =
            AnnotatedClass::class.java.hasAnnotationRecursive(Level1::class.java) &&
                AnnotatedClass::class.java.hasAnnotationRecursive(Level2::class.java) &&
                AnnotatedClass::class.java.hasAnnotationRecursive(Level3::class.java)
        assertTrue(result, "Expected all annotation levels to be found recursively")
    }

    @Test
    fun `should return false if annotation is not present`() {
        class PlainClass

        val result = PlainClass::class.java.hasAnnotationRecursive(Level1::class.java)
        assertFalse(result)
    }
}
