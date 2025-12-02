package io.mockk.junit5

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Meta-annotations for testing recursive annotation lookup
@MockKExtension.CheckUnnecessaryStub(enabled = false)
annotation class MetaCheckUnnecessaryStubDisabled

@MockKExtension.ConfirmVerification(enabled = false)
annotation class MetaConfirmVerificationDisabled

class MockKExtensionOptOutTest {

    @Test
    fun `findAnnotationRecursive should find annotation with default enabled value`() {
        @MockKExtension.CheckUnnecessaryStub
        class AnnotatedClass

        val annotation = AnnotatedClass::class.java.findAnnotationRecursive(MockKExtension.CheckUnnecessaryStub::class.java)
        
        assertTrue(annotation != null, "Expected annotation to be found")
        assertTrue(annotation!!.enabled, "Expected default enabled value to be true")
    }

    @Test
    fun `findAnnotationRecursive should find annotation with enabled set to false`() {
        @MockKExtension.CheckUnnecessaryStub(enabled = false)
        class AnnotatedClass

        val annotation = AnnotatedClass::class.java.findAnnotationRecursive(MockKExtension.CheckUnnecessaryStub::class.java)
        
        assertTrue(annotation != null, "Expected annotation to be found")
        assertFalse(annotation!!.enabled, "Expected enabled value to be false")
    }

    @Test
    fun `findAnnotationRecursive should find annotation with enabled set to true`() {
        @MockKExtension.CheckUnnecessaryStub(enabled = true)
        class AnnotatedClass

        val annotation = AnnotatedClass::class.java.findAnnotationRecursive(MockKExtension.CheckUnnecessaryStub::class.java)
        
        assertTrue(annotation != null, "Expected annotation to be found")
        assertTrue(annotation!!.enabled, "Expected enabled value to be true")
    }

    @Test
    fun `findAnnotationRecursive should return null if annotation is not present`() {
        class PlainClass

        val annotation = PlainClass::class.java.findAnnotationRecursive(MockKExtension.CheckUnnecessaryStub::class.java)
        
        assertNull(annotation, "Expected annotation to not be found")
    }

    @Test
    fun `ConfirmVerification annotation should have default enabled value of true`() {
        @MockKExtension.ConfirmVerification
        class AnnotatedClass

        val annotation = AnnotatedClass::class.java.findAnnotationRecursive(MockKExtension.ConfirmVerification::class.java)
        
        assertTrue(annotation != null, "Expected annotation to be found")
        assertTrue(annotation!!.enabled, "Expected default enabled value to be true")
    }

    @Test
    fun `ConfirmVerification annotation should support enabled set to false`() {
        @MockKExtension.ConfirmVerification(enabled = false)
        class AnnotatedClass

        val annotation = AnnotatedClass::class.java.findAnnotationRecursive(MockKExtension.ConfirmVerification::class.java)
        
        assertTrue(annotation != null, "Expected annotation to be found")
        assertFalse(annotation!!.enabled, "Expected enabled value to be false")
    }

    @Test
    fun `findAnnotationRecursive should find CheckUnnecessaryStub via meta-annotation`() {
        @MetaCheckUnnecessaryStubDisabled
        class AnnotatedClass

        val annotation = AnnotatedClass::class.java.findAnnotationRecursive(MockKExtension.CheckUnnecessaryStub::class.java)
        
        assertTrue(annotation != null, "Expected annotation to be found via meta-annotation")
        assertFalse(annotation!!.enabled, "Expected enabled value from meta-annotation to be false")
    }

    @Test
    fun `findAnnotationRecursive should find ConfirmVerification via meta-annotation`() {
        @MetaConfirmVerificationDisabled
        class AnnotatedClass

        val annotation = AnnotatedClass::class.java.findAnnotationRecursive(MockKExtension.ConfirmVerification::class.java)
        
        assertTrue(annotation != null, "Expected annotation to be found via meta-annotation")
        assertFalse(annotation!!.enabled, "Expected enabled value from meta-annotation to be false")
    }
}
