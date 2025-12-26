package io.mockk.impl.restrict

import io.mockk.MockKException
import io.mockk.impl.log.Logger
import kotlin.reflect.KClass

class MockkValidator(
    private val configuration: RestrictMockkConfiguration,
) {
    private val logger by lazy { Logger<MockkValidator>() }
    private val restrictedClasses: Set<String> = configuration.restrictedTypes

    fun validateMockableClass(clazz: KClass<*>) {
        if (isRestrictedClass(clazz)) {
            logger.warn { "${clazz.simpleName} should not be mocked! Consider refactoring your test." }

            if (configuration.throwExceptionOnBadMock) {
                throw MockKException("Mocking ${clazz.qualifiedName} is not allowed!")
            }
        }
    }

    private fun isRestrictedClass(clazz: KClass<*>): Boolean {
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
}
