package io.mockk.junit5

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.AdditionalInterface
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.junit.jupiter.api.extension.*
import java.lang.annotation.Inherited
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Parameter
import java.util.Optional

/**
 * JUnit5 extension.
 *
 * Parameters can use [MockK] and [RelaxedMockK].
 * Class properties can use [MockK], [RelaxedMockK] and [SpyK]
 * [unmockkAll] will be called after test class execution (*)
 *
 * Usage: declare @ExtendWith(MockKExtension.class) on a test class
 *
 * Alternatively –Djunit.extensions.autodetection.enabled=true may be placed on a command line.
 *
 * (*) [unmockkAll] default behavior can be disabled by adding [KeepMocks] to your test class or method or
 * –Dmockk.junit.extension.keepmocks=true on a command line
 */
class MockKExtension : TestInstancePostProcessor, ParameterResolver, AfterAllCallback {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return getMockKAnnotation(parameterContext) != null
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        val parameter = parameterContext.parameter
        val type = parameter.type.kotlin
        val annotation = getMockKAnnotation(parameterContext) ?: return null
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
            *moreInterfaces(parameterContext),
            relaxUnitFun = isRelaxedUnitFun
        )
    }

    private fun getMockKAnnotation(parameter: ParameterContext): Any? {
        return sequenceOf(MockK::class, RelaxedMockK::class)
            .map { parameter.findAnnotation(it.java) }
            .firstOrNull { it.isPresent }
            ?.get()
    }

    private fun getMockName(parameter: Parameter, annotation: Any): String? {
        return when {
            annotation is MockK -> annotation.name
            annotation is RelaxedMockK -> annotation.name
            parameter.isNamePresent -> parameter.name
            else -> null
        }
    }

    private fun moreInterfaces(parameter: ParameterContext) =
        parameter.findAnnotation(AdditionalInterface::class.java)
            .map { it.type }
            .map { arrayOf(it) }
            .orElseGet { emptyArray() }

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        MockKAnnotations.init(testInstance)
    }

    override fun afterAll(context: ExtensionContext) {
        if (!context.keepMocks) {
            unmockkAll()
        }
    }

    private val ExtensionContext.keepMocks: Boolean
        get() = testClass.keepMocks || testMethod.keepMocks ||
                getConfigurationParameter(KEEP_MOCKS_PROPERTY).map { it.toBoolean() }.orElse(false)

    private val Optional<out AnnotatedElement>.keepMocks
        get() = map { it.getAnnotation(KeepMocks::class.java) != null }
            .orElse(false)

    /***
     * Prevent calling [unmockkAll] after each test execution
     */
    @Inherited
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    annotation class KeepMocks

    companion object {
        const val KEEP_MOCKS_PROPERTY = "mockk.junit.extension.keepmocks"
    }

}
