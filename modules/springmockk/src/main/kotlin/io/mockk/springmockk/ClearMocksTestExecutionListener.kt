package io.mockk.springmockk

import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.Ordered
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.util.ClassUtils

/**
 * `TestExecutionListener` to reset any mock beans that have been marked with a
 * [MockkClear]. Typically used alongside [MockkTestExecutionListener].
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @since 1.4.0
 * @see MockkTestExecutionListener
 */
class ClearMocksTestExecutionListener : AbstractTestExecutionListener() {
    private val MOCKK_IS_PRESENT = ClassUtils.isPresent(
        "io.mockk.MockK",
        ClearMocksTestExecutionListener::class.java.classLoader
    )

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE - 100
    }

    override fun beforeTestMethod(testContext: TestContext) {
        if (MOCKK_IS_PRESENT) {
            clearMocks(testContext.applicationContext, MockkClear.BEFORE)
        }
    }

    override fun afterTestMethod(testContext: TestContext) {
        if (MOCKK_IS_PRESENT) {
            clearMocks(testContext.applicationContext, MockkClear.AFTER)
        }
    }

    private fun clearMocks(applicationContext: ApplicationContext, clear: MockkClear) {
        if (applicationContext is ConfigurableApplicationContext) {
            clearMocks(applicationContext, clear)
        }
    }

    private fun clearMocks(applicationContext: ConfigurableApplicationContext, clear: MockkClear) {
        val beanFactory = applicationContext.beanFactory
        val names = beanFactory.beanDefinitionNames
        val instantiatedSingletons = beanFactory.singletonNames.toSet()
        for (name in names) {
            val definition = beanFactory.getBeanDefinition(name)
            if (definition.isSingleton && instantiatedSingletons.contains(name)) {
                val bean = beanFactory.getSingleton(name)
                bean?.let {
                    if (clear == MockkClear.get(bean)) {
                        io.mockk.clearMocks(bean)
                    }
                }
            }
        }
        try {
            val mockkCreatedBeans = beanFactory.getBean(MockkCreatedBeans::class.java)
            for (bean in mockkCreatedBeans) {
                if (clear == MockkClear.get(bean)) {
                    io.mockk.clearMocks(bean)
                }
            }
        } catch (ex: NoSuchBeanDefinitionException) {
            // Continue
        }

        applicationContext.parent ?.let {
            clearMocks(it, clear)
        }
    }
}
