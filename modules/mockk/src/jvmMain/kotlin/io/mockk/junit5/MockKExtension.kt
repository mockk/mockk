package io.mockk.junit5

import io.mockk.*
import io.mockk.impl.annotations.AdditionalInterface
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.*
import java.lang.annotation.Inherited
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Parameter
import java.util.Optional
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaConstructor

/**
 * MockK JUnit5 extension.
 *
 * Parameters can use [MockK] and [RelaxedMockK].
 * Class properties can use [MockK], [RelaxedMockK] and [SpyK].
 *
 * [unmockkAll] will be called after each test function execution if the test lifecycle is set to
 * [TestInstance.Lifecycle.PER_METHOD] (JUnit 5 default), or after all test functions in the test class have been
 * executed if the test lifecycle is set to [TestInstance.Lifecycle.PER_CLASS].
 * [unmockkAll] will not be called if the [KeepMocks] annotation is present or if the `mockk.junit.extension.keepmocks`
 * Gradle property is set to `true`.
 *
 * Usage: declare `@ExtendWith(MockKExtension::class)` on a test class.
 *
 * Alternatively `–Djunit.extensions.autodetection.enabled=true` may be placed on a command line.
 */
class MockKExtension : TestInstancePostProcessor, ParameterResolver, AfterEachCallback, AfterAllCallback {
    private val cache = mutableMapOf<KClass<out Any>, Any>()

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return getMockKAnnotation(parameterContext) != null
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        val parameter = parameterContext.parameter
        val type = parameter.type.kotlin
        val annotation = getMockKAnnotation(parameterContext) ?: return null
        val name = getMockName(parameterContext.parameter, annotation)

        return when (annotation) {
            is InjectMockKs -> tryConstructClass(type)
            is SpyK -> spyk(
                tryConstructClass(type),
                name,
                *moreInterfaces(parameterContext),
                recordPrivateCalls = annotation.recordPrivateCalls
            )

            is MockK, is RelaxedMockK -> {
                mockkClass(
                    type,
                    name,
                    (annotation as? MockK)?.relaxed ?: true,
                    *moreInterfaces(parameterContext),
                    relaxUnitFun = (annotation as? MockK)?.relaxUnitFun ?: false,
                )
            }

            else -> null
        }?.also { cache[type] = it }
    }

    private fun tryConstructClass(type: KClass<out Any>): Any = try {
        val ctor = type.constructors.first()
        val args = ctor.javaConstructor?.parameters
            ?.map { cache[it.type.kotlin] }
            ?.toTypedArray()
            ?: emptyArray()
        ctor.call(*args)
    } catch (ex: InvocationTargetException) {
        // Current JUnit5 implementation resolves test constructor parameters one-by-one in order of declaration.
        // This means that any parameter can only access preceding arguments and only those can be used to
        // construct non-mock class instance. Breaking this order will cause NPE at test class initialization.
        // Same limitation also applies to spies as their use original class implementation under hood.
        throw MockKException(
            "Unable to instantiate class ${type.simpleName}. " +
                    "Please ensure that all dependencies needed by class are defined before it in test class constructor. " +
                    "Already registered mocks: ${cache.values}", ex
        )
    }

    private fun getMockKAnnotation(parameter: ParameterContext): Any? {
        return sequenceOf(MockK::class, RelaxedMockK::class, SpyK::class, InjectMockKs::class)
            .map { parameter.findAnnotation(it.java) }
            .firstOrNull { it.isPresent }
            ?.get()
    }

    private fun getMockName(parameter: Parameter, annotation: Any): String? {
        return when {
            annotation is MockK -> annotation.name
            annotation is RelaxedMockK -> annotation.name
            annotation is SpyK -> annotation.name
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

    override fun afterEach(context: ExtensionContext) {
        if (context.isTestInstanceLifecyclePerMethod) {
            finish(context)
        }
    }

    override fun afterAll(context: ExtensionContext) {
        if (!context.isTestInstanceLifecyclePerMethod) {
            finish(context)
        }
    }

    private fun finish(context: ExtensionContext) {
        if (!context.keepMocks && !context.requireParallelTesting) {
            unmockkAll()
        }

        try {
            if (context.confirmVerification) {
                confirmVerified()
            }

            if (context.checkUnnecessaryStub) {
                checkUnnecessaryStub()
            }
        } finally {
            // Clear all mocks after missed verifications or unnecessary stubs. Solves Issue #963.
            if (!context.keepMocks && !context.requireParallelTesting) {
                clearAllMocks()
            }
        }
    }

    private val ExtensionContext.isTestInstanceLifecyclePerMethod: Boolean
        get() = testInstanceLifecycle.map { it == TestInstance.Lifecycle.PER_METHOD }.orElse(true)

    private val ExtensionContext.keepMocks: Boolean
        get() = testClass.keepMocks || testMethod.keepMocks ||
                getConfigurationParameter(KEEP_MOCKS_PROPERTY).map { it.toBoolean() }.orElse(false)

    private val Optional<out AnnotatedElement>.keepMocks
        get() = map { it.getAnnotation(KeepMocks::class.java) != null }
            .orElse(false)

    private val ExtensionContext.confirmVerification: Boolean
        get() = testClass.confirmVerification ||
                getConfigurationParameter(CONFIRM_VERIFICATION_PROPERTY).map { it.toBoolean() }.orElse(false)

    private val Optional<out AnnotatedElement>.confirmVerification
        get() = map { it.getAnnotation(ConfirmVerification::class.java) != null }
            .orElse(false)

    private val ExtensionContext.checkUnnecessaryStub: Boolean
        get() = testClass.checkUnnecessaryStub ||
                getConfigurationParameter(CHECK_UNNECESSARY_STUB_PROPERTY).map { it.toBoolean() }.orElse(false)

    private val Optional<out AnnotatedElement>.checkUnnecessaryStub
        get() = map { it.getAnnotation(CheckUnnecessaryStub::class.java) != null }
            .orElse(false)

    private val ExtensionContext.requireParallelTesting: Boolean
        get() = testClass.requireParallelTesting ||
                getConfigurationParameter(REQUIRE_PARALLEL_TESTING).map { it.toBoolean() }.orElse(false)

    private val Optional<out AnnotatedElement>.requireParallelTesting
        get() = map { it.getAnnotation(RequireParallelTesting::class.java) != null }
            .orElse(false)

    /***
     * Prevent calling [unmockkAll] after each test execution
     */
    @Inherited
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    annotation class KeepMocks

    @Inherited
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS)
    annotation class ConfirmVerification

    @Inherited
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS)
    annotation class CheckUnnecessaryStub

    /***
     * Require parallel testing by disabling the clearAllMocks call after each test execution; it is not thread-safe.
     */
    @Inherited
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS)
    annotation class RequireParallelTesting

    companion object {
        const val KEEP_MOCKS_PROPERTY = "mockk.junit.extension.keepmocks"
        const val CONFIRM_VERIFICATION_PROPERTY = "mockk.junit.extension.confirmverification"
        const val CHECK_UNNECESSARY_STUB_PROPERTY = "mockk.junit.extension.checkUnnecessaryStub"
        const val REQUIRE_PARALLEL_TESTING = "mockk.junit.extension.requireParallelTesting"
    }
}
