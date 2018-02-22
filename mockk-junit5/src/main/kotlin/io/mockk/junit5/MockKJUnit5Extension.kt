package io.mockk.junit5

import io.mockk.MockKAnnotations
import io.mockk.MockKGateway
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.lang.reflect.Parameter

/**
 * JUnit5 extension based on the Mockito extension found in JUnit5's samples
 */
class MockKJUnit5Extension : TestInstancePostProcessor, ParameterResolver {
    //TODO: Support other annotations (@Spy. @RelaxedMockK)
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.isAnnotationPresent(MockK::class.java)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return getMock(parameterContext.parameter, extensionContext)
    }

    private fun getMock(parameter: Parameter, extensionContext: ExtensionContext): Any {
        val mockType = parameter.type
        val mocks = extensionContext.getStore(ExtensionContext.Namespace.create(MockKJUnit5Extension::class.java, mockType))
        val mockName = getMockName(parameter, mockType)

        return mocks.getOrComputeIfAbsent(mockName) { key ->
            io.mockk.MockK.useImpl {
                MockKGateway.implementation().mockFactory.mockk(mockType.kotlin, key, false, arrayOf())
            }
        }
    }

    private fun getMockName(parameter: Parameter, mockType: Class<*>): String {
        val explicitMockName = parameter.getAnnotation(MockK::class.java).name.trim()

        return when {
            explicitMockName.isNotEmpty() -> explicitMockName
            parameter.isNamePresent -> parameter.name
            else -> return mockType.canonicalName
        }
    }

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        MockKAnnotations.init(testInstance)
    }
}
