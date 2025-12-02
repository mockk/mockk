package com.ninjasquad.springmockk

import io.mockk.MockKAnnotations
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Field
import kotlin.reflect.full.memberProperties

/**
 * `TestExecutionListener` to enable [@MockkBean][MockkBean] and [@SpykBean][SpykBean] support.
 * Also triggers [MockKAnnotations#init] when any MockK annotations used.
 *
 * To use the automatic reset support of `@MockkBean` and `@SpykBean`, configure
 * [ClearMocksTestExecutionListener] as well.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author JB Nizet
 * @see ClearMocksTestExecutionListener
 */
class MockkTestExecutionListener : AbstractTestExecutionListener() {
    override fun getOrder(): Int {
        return 1950
    }

    override fun prepareTestInstance(testContext: TestContext) {
        initMocks(testContext)
        injectFields(testContext)
    }

    override fun beforeTestMethod(testContext: TestContext) {
        if (testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE) == true) {
            initMocks(testContext)
            reinjectFields(testContext)
        }
    }

    private fun initMocks(testContext: TestContext) {
        if (hasMockkAnnotations(testContext)) {
            MockKAnnotations.init(testContext.testInstance)
        }
    }

    private fun hasMockkAnnotations(testContext: TestContext): Boolean {
        return testContext.testClass.kotlin.memberProperties.any { property ->
            property.annotations.any {
                it.annotationClass.java.name.startsWith("io.mockk")
            }
        }
    }

    private fun injectFields(testContext: TestContext) {
        postProcessFields(testContext) { mockkField, postProcessor ->
          postProcessor.inject(
              mockkField.field,
              mockkField.target,
              mockkField.definition
          )
      }
    }

    private fun reinjectFields(testContext: TestContext) {
        postProcessFields(testContext) { mockkField, postProcessor ->
            ReflectionUtils.makeAccessible(mockkField.field)
            ReflectionUtils.setField(
                mockkField.field, testContext.testInstance,
                null
            )
            postProcessor.inject(
                mockkField.field, mockkField.target,
                mockkField.definition
            )
        }
    }

    private fun postProcessFields(
        testContext: TestContext,
        consumer: (MockkField, MockkPostProcessor) -> Unit
    ) {
        val parser = DefinitionsParser()
        parser.parse(testContext.testClass)
        if (!parser.parsedDefinitions.isEmpty()) {
            val postProcessor = testContext.applicationContext.getBean(MockkPostProcessor::class.java)
            for (definition in parser.parsedDefinitions) {
                val field = parser.getField(definition)
                if (field != null) {
                    consumer(MockkField(field, testContext.testInstance, definition), postProcessor)
                }
            }
        }
    }

    private class MockkField(
        val field: Field,
        val target: Any,
        val definition: Definition
    )
}
