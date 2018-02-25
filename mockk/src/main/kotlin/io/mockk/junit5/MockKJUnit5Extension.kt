package io.mockk.junit5

import io.mockk.MockKAnnotations
import io.mockk.MockKGateway
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.lang.reflect.Parameter

/**
 * JUnit5 extension based on the Mockito extension found in JUnit5's samples.
 * Allows using the [MockK] and [RelaxedMockK] on class properties and test function parameters,
 * as well as [SpyK] on class properties.
 */
class MockKJUnit5Extension : TestInstancePostProcessor, ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val parameter = parameterContext.parameter
        return parameter.isAnnotationPresent(MockK::class.java)
                || parameter.isAnnotationPresent(RelaxedMockK::class.java)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        return getMock(parameterContext.parameter, extensionContext)
    }

    private fun getMock(parameter: Parameter, extensionContext: ExtensionContext): Any? {
        val mockType = parameter.type

        return getMockKAnnotation(parameter)?.let { annotation ->
            val mockName = getMockName(parameter, mockType, annotation)
            io.mockk.MockK.useImpl {
                MockKGateway.implementation().mockFactory.mockk(mockType.kotlin, mockName, annotation is RelaxedMockK, arrayOf())
            }
        }
    }

    private fun getMockKAnnotation(parameter: Parameter): Any? {
        return parameter.getAnnotation(MockK::class.java) ?: parameter.getAnnotation(RelaxedMockK::class.java)
    }

    private fun getMockName(parameter: Parameter, mockType: Class<*>, annotation: Any): String {
        val fromAnnotation = when (annotation) {
            is MockK -> annotation.name.trim()
            is RelaxedMockK -> annotation.name.trim()
            else -> ""
        }

        return when {
            fromAnnotation.isNotEmpty() -> fromAnnotation
            parameter.isNamePresent -> parameter.name
            else -> mockType.canonicalName
        }
    }

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        MockKAnnotations.init(testInstance)
    }
}
