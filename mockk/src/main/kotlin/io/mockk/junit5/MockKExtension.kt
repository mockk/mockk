package io.mockk.junit5

import io.mockk.MockKAnnotations
import io.mockk.classMockk
import io.mockk.impl.annotations.AdditionalInterface
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.lang.reflect.Parameter

/**
 * JUnit5 extension.
 *
 * Parameters can use [MockK] and [RelaxedMockK].
 * Class properties can use [MockK], [RelaxedMockK] and [SpyK]
 */
class MockKExtension : TestInstancePostProcessor, ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return getMockKAnnotation(parameterContext.parameter) != null
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        val parameter = parameterContext.parameter
        val type = parameter.type.kotlin
        val annotation = getMockKAnnotation(parameter) ?: return null
        val name = getMockName(parameterContext.parameter, annotation)

        return classMockk(
            type,
            name,
            annotation is RelaxedMockK,
            *moreInterfaces(parameter)
        )
    }

    private fun getMockKAnnotation(parameter: Parameter): Any? {
        return parameter.getAnnotation(MockK::class.java)
                ?: parameter.getAnnotation(RelaxedMockK::class.java)
    }

    private fun getMockName(parameter: Parameter, annotation: Any): String? {
        return when {
            annotation is MockK -> annotation.name
            annotation is RelaxedMockK -> annotation.name
            parameter.isNamePresent -> parameter.name
            else -> null
        }
    }

    private fun moreInterfaces(parameter: Parameter) =
        parameter.annotations
            .filter { it is AdditionalInterface }
            .map { it as AdditionalInterface }
            .map { it.type }
            .toTypedArray()

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        MockKAnnotations.init(testInstance)
    }
}
