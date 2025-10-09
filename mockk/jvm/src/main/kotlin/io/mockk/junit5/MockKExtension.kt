package io.mockk.junit5

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.AdditionalInterface
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.mockkClass
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
 *
 * Usage: declare @ExtendWith(MockKExtension.class) on a test class
 *
 * Alternatively –Djunit.extensions.autodetection.enabled=true may be placed on a command line.
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

        val isRelaxed = when {
            annotation is RelaxedMockK -> true
            annotation is MockK -> annotation.relaxed
            else -> false
        }

        val isRelaxedUnitFun = when {
            annotation is MockK -> annotation.relaxUnitFun
            else -> false
        }

        return mockkClass(
            type,
            name,
            isRelaxed,
            *moreInterfaces(parameter),
            relaxUnitFun = isRelaxedUnitFun
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
