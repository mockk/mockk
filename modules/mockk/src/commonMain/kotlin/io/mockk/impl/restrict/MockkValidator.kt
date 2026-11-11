package io.mockk.impl.restrict

import io.mockk.MockKException
import io.mockk.impl.log.Logger
import kotlin.reflect.KClass

class MockkValidator(
    private val configuration: RestrictMockkConfiguration,
) {
    private val logger by lazy { Logger<MockkValidator>() }
    private val restrictedClasses: Set<String> = configuration.restrictedTypes
    private val restrictedAnnotations: Set<String> = configuration.restrictedAnnotations

    fun validateMockableClass(clazz: KClass<*>) {
        if (isRestrictedClass(clazz)) {
            logger.warn { "${clazz.simpleName} should not be mocked! Consider refactoring your test." }

            if (configuration.throwExceptionOnBadMock) {
                throw MockKException("Mocking ${clazz.qualifiedName} is not allowed!")
            }
        }
    }

    private fun isRestrictedClass(clazz: KClass<*>): Boolean = isRestrictedByName(clazz) || isRestrictedByAnnotation(clazz)

    private fun isRestrictedByName(clazz: KClass<*>): Boolean {
        val className = clazz.qualifiedName ?: return false

        if (className in restrictedClasses) return true

        return restrictedClasses.any { restricted ->
            try {
                val restrictedClass = Class.forName(restricted)
                restrictedClass.isAssignableFrom(clazz.java)
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }

    private fun isRestrictedByAnnotation(clazz: KClass<*>): Boolean {
        if (restrictedAnnotations.isEmpty()) return false

        val annotations = clazz.java.annotations

        return restrictedAnnotations.any { restricted ->
            try {
                val annotationClass = Class.forName(restricted)
                annotations.any { annotationClass.isInstance(it) }
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }
}
